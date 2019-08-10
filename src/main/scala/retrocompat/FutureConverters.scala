/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */
package retrocompat

import java8.util.concurrent.CompletionStage

import scala.concurrent.Future

/** This object provides extension methods that convert between Scala [[Future]] and Java
  * [[CompletionStage]]
  *
  * When writing Java code, use the explicit conversion methods defined in
  * [[retrocompat.JavaFutureConverters]] instead.
  *
  * Note that the bridge is implemented at the read-only side of asynchronous handles, namely
  * [[Future]] (instead of [[scala.concurrent.Promise]]) and [[CompletionStage]] (instead of
  * [[java.util.concurrent.CompletableFuture]]). This is intentional, as the semantics of bridging
  * the write-handles would be prone to race conditions; if both ends (`CompletableFuture` and
  * `Promise`) are completed independently at the same time, they may contain different values
  * afterwards. For this reason, `toCompletableFuture` is not supported on the created
  * `CompletionStage`s.
  */
object FutureConverters {
  implicit class FutureOps[T](private val f: Future[T]) extends AnyVal {
    /** Convert a Scala Future to a Java CompletionStage, see [[retrocompat.JavaFutureConverters.asJava]]. */
    def asJava: CompletionStage[T] = retrocompat.JavaFutureConverters.asJava(f)
  }

  implicit class CompletionStageOps[T](private val cs: CompletionStage[T]) extends AnyVal {
    /** Convert a Java CompletionStage to a Scala Future, see [[retrocompat.JavaFutureConverters.asScala]]. */
    def asScala: Future[T] = retrocompat.JavaFutureConverters.asScala(cs)
  }
}
