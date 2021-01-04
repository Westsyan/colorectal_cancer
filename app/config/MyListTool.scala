package config

import java.io.File

import shapeless.ops.hlist.ToList
import shapeless.ops.record.{Keys, Values}
import shapeless.{HList, LabelledGeneric}
import utils.Utils

/**
 * Created by Administrator on 2019/11/13
 */
trait MyListTool {

  implicit class MyList[T](list: List[T]) {

    def distinctBy[B](f: T => B) = {
      list.map { x =>
        (f(x), x)
      }.toMap.values.toList
    }

    def distinctByKeepHead[B](f: T => B): List[T] = {

      def loop(list: List[T], acc: (List[T], Set[B])): (List[T], Set[B]) = {
        list match {
          case Nil => acc
          case x :: xs =>
            val key = f(x)
            val (list, set) = acc
            if (!set.contains(key)) {
              loop(xs, (list ::: List(x), set + (key)))
            } else loop(xs, acc)
        }
      }

      loop(list, (List[T](), Set[B]()))._1
    }

    def lines[R <: HList, K <: HList, V <: HList](
                                                   implicit gen: LabelledGeneric.Aux[T, R], keys: Keys.Aux[R, K],
                                                   values: Values.Aux[R, V],
                                                   ktl: ToList[K, Symbol],
                                                   vtl: ToList[V, Any]
                                                 ) = {
      Utils.getLinesByTs(list)
    }

    def toTxtFile[R <: HList, K <: HList, V <: HList](file: File)(
      implicit gen: LabelledGeneric.Aux[T, R], keys: Keys.Aux[R, K],
      values: Values.Aux[R, V],
      ktl: ToList[K, Symbol],
      vtl: ToList[V, Any]
    ) = {
      Utils.getLinesByTs(list)
    }


  }


}
