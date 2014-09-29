package au.com.dius.pact.matchers

import au.com.dius.pact.model.JsonDiff._
import au.com.dius.pact.model.{BodyMismatch, HttpPart}
import org.json4s.JsonAST.JObject
import org.json4s.{JValue, DefaultFormats}
import org.json4s.jackson.JsonMethods._

class JsonBodyMatcher extends BodyMatcher {
  implicit lazy val formats = DefaultFormats

  def matchBody(expected: HttpPart, actual: HttpPart, diffConfig: DiffConfig): List[BodyMismatch] = {
    (expected.body, actual.body) match {
      case (None, None) => List()
      case (None, b) => if (diffConfig.structural) {
        List()
      } else {
        List(BodyMismatch(None, b))
      }
      case (a, None) => List(BodyMismatch(a, None))
      case (Some(a), Some(b)) => compare("/", parse(a), parse(b))
    }
  }

  def valueOf(value: Any) = {
    if (value.isInstanceOf[String]) {
      s"'$value'"
    } else {
      value.toString
    }
  }

  def typeOf(value: Any) = {
    if (value == null) {
      "Null"
    } else value match {
      case _: Map[Any, Any] =>
        "Map"
      case _: List[Any] =>
        "List"
      case _ =>
        value.getClass.getSimpleName
    }
  }

  def compare(path: String, expected: Any, actual: Any): List[BodyMismatch] = {
    if (expected != actual) {
      (expected, actual) match {
        case (a: JObject, b: JObject) =>
          val expectedValues: Map[String, Any] = a.values
          val actualValues: Map[String, Any] = b.values
          if (expectedValues.isEmpty && actualValues.nonEmpty) {
            List(BodyMismatch(a, b, Some(s"Expected an empty Map but received ${valueOf(actual)}"), path))
          } else {
            var result = List[BodyMismatch]()
            if (expectedValues.size > actualValues.size) {
              result = result :+ BodyMismatch(a, b, Some(s"Expected a Map with at least ${expectedValues.size} elements but received ${actualValues.size} elements"), path)
            }
            expectedValues.foreach(entry => {
              val s = path + entry._1 + "/"
              if (actualValues.contains(entry._1)) {
                result = result ++: compare(s, entry._2, actualValues(entry._1))
              } else {
                result = result :+ BodyMismatch(a, b, Some(s"Expected ${entry._1}=${valueOf(entry._2)} but was missing"), path)
              }
            })
            result
          }
//        case (a: List[Any], b: List[Any]) =>
//          if (a.isEmpty && b.nonEmpty) {
//            List(BodyMismatch(a, b, Some(s"Expected an empty List but received ${valueOf(actual)}"), path))
//          } else {
//            var result = List[BodyMismatch]()
//            if (a.size != b.size) {
//              result = result :+ BodyMismatch(a, b, Some(s"Expected a List with ${a.size} elements but received ${b.size} elements"), path)
//            }
//            for ((value, index) <- a.view.zipWithIndex) {
//              val s = path + index + "/"
//              if (index < b.size) {
//                result = result ++: compare(s, value, b(index))
//              } else {
//                result = result :+ BodyMismatch(a, b, Some(s"Expected ${valueOf(value)} but was missing"), path)
//              }
//            }
//            result
//          }
//        case (_, _) =>
//          if ((expected.isInstanceOf[Map[Any, Any]] && !actual.isInstanceOf[Map[Any, Any]]) ||
//            (expected.isInstanceOf[List[Any]] && !actual.isInstanceOf[List[Any]])) {
//            List(BodyMismatch(expected, actual, Some(s"Type mismatch: Expected ${typeOf(expected)} ${valueOf(expected)} but received ${typeOf(actual)} ${valueOf(actual)}"), path))
//          } else {
//            List(BodyMismatch(expected, actual, Some(s"Expected ${valueOf(expected)} but received ${valueOf(actual)}"), path))
//          }
      }
    } else {
      List[BodyMismatch]()
    }
  }
}