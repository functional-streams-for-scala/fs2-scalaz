package fs2.interop.scalaz

import fs2.util._
import _root_.scalaz.{ \/, Catchable => ZCatchable, Functor => ZFunctor, Monad => ZMonad, MonadError, NaturalTransformation }

trait Instances extends Instances0 {
  implicit def effectToMonadError[F[_]](implicit F: Effect[F]): MonadError[F, Throwable] = new MonadError[F, Throwable] {
    def point[A](a: => A) = F.delay(a)
    override def map[A, B](fa: F[A])(f: A => B) = F.map(fa)(f)
    def bind[A, B](fa: F[A])(f: A => F[B]) = F.flatMap(fa)(f)
    def raiseError[A](t: Throwable) = F.fail(t)
    def handleError[A](fa: F[A])(f: Throwable => F[A]) = F.flatMap(F.attempt(fa))(e => e.fold(f, F.pure))
  }

  implicit def uf1ToNatrualTransformation[F[_], G[_]](uf1: UF1[F, G]): NaturalTransformation[F, G] = new NaturalTransformation[F, G] {
    def apply[A](fa: F[A]) = uf1(fa)
  }
}

private[scalaz] trait Instances0 extends Instances1 {
  implicit def catchableToMonadError[F[_]](implicit F: Catchable[F]): MonadError[F, Throwable] = new MonadError[F, Throwable] {
    def point[A](a: => A) = F.pure(a)
    override def map[A, B](fa: F[A])(f: A => B) = F.map(fa)(f)
    def bind[A, B](fa: F[A])(f: A => F[B]) = F.flatMap(fa)(f)
    def raiseError[A](t: Throwable) = F.fail(t)
    def handleError[A](fa: F[A])(f: Throwable => F[A]) = F.flatMap(F.attempt(fa))(e => e.fold(f, F.pure))
  }

  implicit def catchableToScalaz[F[_]](implicit F: Catchable[F]): ZCatchable[F] = new ZCatchable[F] {
    def fail[A](t: Throwable) = F.fail(t)
    def attempt[A](fa: F[A]) = F.map(F.attempt(fa))(\/.fromEither)
  }
}

private[scalaz] trait Instances1 extends Instances2 {
  implicit def monadToScalaz[F[_]](implicit F: Monad[F]): ZMonad[F] = new ZMonad[F] {
    def point[A](a: => A) = F.pure(a)
    override def map[A, B](fa: F[A])(f: A => B) = F.map(fa)(f)
    def bind[A, B](fa: F[A])(f: A => F[B]) = F.flatMap(fa)(f)
  }
}

private[scalaz] trait Instances2 {

  implicit def functorToScalaz[F[_]](implicit F: Functor[F]): ZFunctor[F] = new ZFunctor[F] {
    def map[A, B](fa: F[A])(f: A => B) = F.map(fa)(f)
  }
}

