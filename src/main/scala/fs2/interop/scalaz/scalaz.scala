package fs2
package interop

import _root_.scalaz.{ Monoid, Semigroup }

import _root_.scalaz.std.map._
import _root_.scalaz.std.vector._

import fs2.util.{ Catchable, Free }

package object scalaz extends Instances with TaskAsyncInstances {

  object reverse extends ReverseInstances

  implicit class StreamScalazOps[F[_], A](val self: Stream[F, A]) extends AnyVal {

    def foldMap[B](f: A => B)(implicit M: Monoid[B]): Stream[F, B] =
      self.fold(M.zero)((b, a) => M.append(b, f(a)))

    def foldMonoid(implicit M: Monoid[A]): Stream[F, A] =
      self.fold(M.zero)(M.append(_, _))

    def foldSemigroup(implicit S: Semigroup[A]): Stream[F, A] =
      self.reduce(S.append(_, _))

    def runFoldMapFree[B](f: A => B)(implicit M: Monoid[B]): Free[F, B] =
      self.runFoldFree(M.zero)((b, a) => M.append(b, f(a)))

    def runGroupByFoldMapFree[K, B: Monoid](f: A => K)(g: A => B): Free[F, Map[K, B]] =
      runFoldMapFree(a => Map(f(a) -> g(a)))

    def runGroupByFoldMonoidFree[K](f: A => K)(implicit M: Monoid[A]): Free[F, Map[K, A]] =
      runFoldMapFree(a => Map(f(a) -> a))

    def runGroupByFree[K](f: A => K)(implicit M: Monoid[A]): Free[F, Map[K, Vector[A]]] =
      runGroupByFoldMapFree(f)(a => Vector(a))

    def runFoldMap[B](f: A => B)(implicit F: Catchable[F], M: Monoid[B]): F[B] =
      runFoldMapFree(f).run

    def runGroupByFoldMap[K, B: Monoid](f: A => K)(g: A => B)(implicit F: Catchable[F]): F[Map[K, B]] =
      runGroupByFoldMapFree(f)(g).run

    def runGroupByFoldMonoid[K](f: A => K)(implicit F: Catchable[F], M: Monoid[A]): F[Map[K, A]] =
      runGroupByFoldMonoidFree(f).run

    def runGroupBy[K](f: A => K)(implicit F: Catchable[F], M: Monoid[A]): F[Map[K, Vector[A]]] =
      runGroupByFree(f).run

  }

}

