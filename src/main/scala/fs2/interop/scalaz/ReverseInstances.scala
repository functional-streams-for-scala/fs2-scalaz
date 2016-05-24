package fs2.interop.scalaz

import fs2.util._
import _root_.scalaz.{ Functor => ZFunctor, Monad => ZMonad, MonadError, NaturalTransformation }

trait ReverseInstances extends ReverseInstances0 {

  implicit def monadErrorToCatchable[F[_]](implicit F: MonadError[F, Throwable]): Catchable[F] = new Catchable[F] {
    def pure[A](a: A) = F.point(a)
    override def map[A, B](fa: F[A])(f: A => B) = F.map(fa)(f)
    def bind[A, B](fa: F[A])(f: A => F[B]) = F.bind(fa)(f)
    def fail[A](t: Throwable) = F.raiseError(t)
    def attempt[A](fa: F[A]) = F.handleError(F.map(fa)(a => Right(a): Either[Throwable, A]))(t => pure(Left(t)))
  }

  implicit def naturalTransformationToUf1[F[_], G[_]](implicit nt: NaturalTransformation[F, G]): UF1[F, G] = new UF1[F, G] {
    def apply[A](fa: F[A]) = nt(fa)
  }
}

private[scalaz] trait ReverseInstances0 extends ReverseInstances1 {

  implicit def scalazToMonad[F[_]](implicit F: ZMonad[F]): Monad[F] = new Monad[F] {
    def pure[A](a: A) = F.point(a)
    override def map[A, B](fa: F[A])(f: A => B) = F.map(fa)(f)
    def bind[A, B](fa: F[A])(f: A => F[B]) = F.bind(fa)(f)
  }
}

private[scalaz] trait ReverseInstances1 {

  implicit def scalazToFunctor[F[_]](implicit F: ZFunctor[F]): Functor[F] = new Functor[F] {
    def map[A, B](fa: F[A])(f: A => B) = F.map(fa)(f)
  }
}

