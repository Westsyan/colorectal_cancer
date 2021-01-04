package config

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable}

trait MyAwait {

  implicit class MyAwait[T](awaitable: Awaitable[T]) {

    def toAwait:T = {
      Await.result(awaitable,Duration.Inf)
    }
  }

}
