package fs2.interop.scalaz

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }

import fs2.Async
import fs2.internal.LinkedMap
import fs2.util.Free
import _root_.scalaz.\/
import _root_.scalaz.concurrent.{ Strategy, Task, Actor }
import _root_.scalaz.syntax.either._


trait TaskAsyncInstances {

  implicit def asyncInstance(implicit S:Strategy): Async[Task] = new Async[Task] {
    def ref[A]: Task[Async.Ref[Task,A]] = ScalazTask.ref[A](S)
    def flatMap[A, B](a: Task[A])(f: (A) => Task[B]): Task[B] = a flatMap f
    def pure[A](a: A): Task[A] = Task.now(a)
    override def delay[A](a: => A) = Task.delay(a)
    def suspend[A](fa: => Task[A]) = Task.suspend(fa)
    def fail[A](err: Throwable): Task[A] = Task.fail(err)
    def attempt[A](fa: Task[A]): Task[Either[Throwable, A]] = fa.attempt.map(_.toEither)
    override def toString = "Async[scalaz.concurrent.Task]"
  }

  implicit def runInstance(implicit S:Strategy): Async.Run[Task] = new Async.Run[Task] {
    def unsafeRunAsyncEffects(f: Task[Unit])(onError: Throwable => Unit) = {
      val _ = S(f.unsafePerformAsync(_.fold(onError, _ => ())))
    }
    override def toString = "Run[scalaz.concurrent.Task]"
  }

  /*
   * Implementation is taken from `fs2` library, with only minor changes. See:
   *
   * https://github.com/functional-streams-for-scala/fs2/blob/v0.9.0-M2/core/src/main/scala/fs2/util/Task.scala
   *
   * Copyright (c) 2013 Paul Chiusano, and respective contributors
   *
   * and is licensed MIT, see LICENSE file at:
   *
   * https://github.com/functional-streams-for-scala/fs2/blob/series/0.9/LICENSE
   */
  private[fs2] object ScalazTask {
    private type Callback[A] = \/[Throwable,A] => Unit

    private trait MsgId
    private trait Msg[A]
    private object Msg {
      case class Read[A](cb: Callback[(A, Long)], id: MsgId) extends Msg[A]
      case class Nevermind[A](id: MsgId, cb: Callback[Boolean]) extends Msg[A]
      case class Set[A](r: \/[Throwable,A]) extends Msg[A]
      case class TrySet[A](id: Long, r: \/[Throwable,A],
        cb: Callback[Boolean]) extends Msg[A]
    }

    def ref[A](implicit S: Strategy): Task[Ref[A]] = Task.delay {
      var result: \/[Throwable,A] = null
      // any waiting calls to `access` before first `set`
      var waiting: LinkedMap[MsgId, Callback[(A, Long)]] = LinkedMap.empty
      // id which increases with each `set` or successful `modify`
      var nonce: Long = 0

      lazy val actor: Actor[Msg[A]] = Actor.actor[Msg[A]] {
        case Msg.Read(cb, idf) =>
          if (result eq null) waiting = waiting.updated(idf, cb)
          else { val r = result; val id = nonce; S { cb(r.map((_,id))) }; () }

        case Msg.Set(r) =>
          if (result eq null) {
            nonce += 1L
            val id = nonce
            waiting.values.foreach(cb => S { cb(r.map((_,id))) })
            waiting = LinkedMap.empty
          }
          result = r

        case Msg.TrySet(id, r, cb) =>
          if (id == nonce) {
            nonce += 1L; val id2 = nonce
            waiting.values.foreach(cb => S { cb(r.map((_,id2))) })
            waiting = LinkedMap.empty
            result = r
            cb(true.right)
          }
          else cb(false.right)

        case Msg.Nevermind(id, cb) =>
          val interrupted = waiting.get(id).isDefined
          waiting = waiting - id
          val _ = S { cb (interrupted.right) }
      }

      new Ref(actor)
    }

    class Ref[A] private[fs2](actor: Actor[Msg[A]])(implicit S: Strategy, protected val F: Async[Task]) extends Async.Ref[Task,A] {

      def access: Task[(A, Either[Throwable,A] => Task[Boolean])] =
        Task.delay(new MsgId {}).flatMap { mid =>
          getStamped(mid).map { case (a, id) =>
            val set = (a: Either[Throwable,A]) =>
              Task.async[Boolean] { cb => actor ! Msg.TrySet(id, \/.fromEither(a), cb) }
            (a, set)
          }
        }

      /**
       * Return a `Task` that submits `t` to this ref for evaluation.
       * When it completes it overwrites any previously `put` value.
       */
      def set(t: Task[A]): Task[Unit] =
        Task.delay { S { t.unsafePerformAsync { r => actor ! Msg.Set(r) } }; () }
      def setFree(t: Free[Task,A]): Task[Unit] =
        set(t.run(F))
      def runSet(e: Either[Throwable,A]): Unit =
        actor ! Msg.Set(\/.fromEither(e))

      private def getStamped(msg: MsgId): Task[(A,Long)] =
        Task.async[(A,Long)] { cb => actor ! Msg.Read(cb, msg) }

      /** Return the most recently completed `set`, or block until a `set` value is available. */
      override def get: Task[A] = Task.delay(new MsgId {}).flatMap { mid => getStamped(mid).map(_._1) }

      /** Like `get`, but returns a `Task[Unit]` that can be used cancel the subscription. */
      def cancellableGet: Task[(Task[A], Task[Unit])] = Task.delay {
        val id = new MsgId {}
        val get = getStamped(id).map(_._1)
        val cancel = Task.async[Unit] {
          cb => actor ! Msg.Nevermind(id, r => cb(r.right.map(_ => ())))
        }
        (get, cancel)
      }

      /**
       * Runs `t1` and `t2` simultaneously, but only the winner gets to
       * `set` to this `ref`. The loser continues running but its reference
       * to this ref is severed, allowing this ref to be garbage collected
       * if it is no longer referenced by anyone other than the loser.
       */
      def setRace(t1: Task[A], t2: Task[A]): Task[Unit] = Task.delay {
        val ref = new AtomicReference(actor)
        val won = new AtomicBoolean(false)
        val win = (res: \/[Throwable,A]) => {
          // important for GC: we don't reference this ref
          // or the actor directly, and the winner destroys any
          // references behind it!
          if (won.compareAndSet(false, true)) {
            val actor = ref.get
            ref.set(null)
            actor ! Msg.Set(res)
          }
        }
        t1.unsafePerformAsync(win)
        t2.unsafePerformAsync(win)
      }
    }

  }
}
