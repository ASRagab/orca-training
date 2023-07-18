import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

object ProcessingSerdeResponses {

  /** Given a set of case classes representing a `Request` and `Response`, some `Serde` trait and a dummy client, write
    * the following methods:
    *   1. A method called `processRequests` that takes a single parameter `payloads` which is of type
    *      [[List[Map[String, Int]] ]] and produces a [[Future[List[Either[String, Response]]] ]]; it should call the
    *      somewhat flaky `sendRequest` method on the `DummyClient`
    *
    * 2. Change the method `processRequests` such that it returns `Future[List[Response]]` instead, in other words
    * filter out bad responses
    *
    * 3. Update the method and add any additional functionality so that the type of `payloads` is no longer specialized
    * to `Map[String, Int]` but that the value in the map can be any `T`
    */

  object Solution {
    trait Serde[T] {
      def serialize(value: T): String = value.toString

      def deserialize(value: String): T
    }

    case class Request[T: Serde](payload: Map[String, T])

    case class Response(values: List[String], statusCode: Int)

    object DummyClient {
      def sendRequest[T](req: Request[T])(implicit ev: Serde[T]): Future[Either[String, Response]] = {
        val flaky = scala.util.Random.nextInt(10)
        val values = req.payload.values.map(t => ev.serialize(t)).toList

        flaky match {
          case even if even % 2 == 0 => Future.successful(Right(Response(values, 200)))
          case odd                   => Future.successful(Left(s"request failed: $odd"))
        }

      }
    }

    object implicits {
      implicit val intSerde: Serde[Int] = new Serde[Int] {
        override def deserialize(value: String): Int =
          Try(Integer.parseInt(value, 10)).toOption.getOrElse(42)
      }
    }

    // Solutions Here
    def processRequests(payloads: List[Map[String, Int]]): Future[List[Either[String, Response]]] = ???

    // Sample Runs
    val intPayloads = List(
      Map("string1" -> 1, "string2" -> 2),
      Map("string3" -> 3, "string4" -> 4),
      Map("string5" -> 5, "string6" -> 6),
      Map("string7" -> 7, "string8" -> 8),
      Map("string9" -> 9, "string10" -> 10)
    )

    // For question 3
    val stringPayloads = List(
      Map("string1" -> "1", "string2" -> "2"),
      Map("string3" -> "3", "string4" -> "4"),
      Map("string5" -> "5", "string6" -> "6"),
      Map("string7" -> "7", "string8" -> "8"),
      Map("string9" -> "9", "string10" -> "10")
    )

    def main(args: Array[String]): Unit = {
      println(Await.result(processRequests(intPayloads), Duration.Inf))
      //   println(Await.result(processRequests(stringPayloads), Duration.Inf))
    }
  }

  def main(args: Array[String]): Unit = {
    Solution.main(args)
  }

}

// Serde Response Solutions & Interview Guidelines
//
// This question presents a dummy API to send and process requests and return responses. The goal of the exercise to see
// if the dev can process typical response types (Future, Either, etc.) in a clean and functional way using the tools
// provided by the standard library and whether they have a good enough understanding of implicits to be able to manage
// serializing responses by type. There are three subquestions, each dealing with a different aspect of the problem:
// 1. Getting the responses
// 2. Filtering out bad responses
// 3. Generalizing the method to handle different types of requests.
// I would expect an IC2 Scala dev to answer 1 (10 minutes) maybe take a beat to solve 2 and potentially struggle a bit
// around the third one (implicits/view bounds), I would expect a IC 3.2/4 dev to do pretty well really
// (like all three in 20-30 minutes), as a challenge maybe ask how they might collect the bad responses or how they
// might handle things when the future fails rather than just the invalid response
// (e.g. turned the failed future into a left, etc.).

object Solutions extends App {

  import ProcessingSerdeResponses.Solution._
  import implicits._

  object Solution1 {
    def processRequests(payloads: List[Map[String, Int]]): Future[List[Either[String, Response]]] = {
      // knowing that apply needs a type annotation is tricky here, give them this hint if they run into it
      Future.traverse(payloads.map(Request.apply[Int]))(DummyClient.sendRequest[Int])
    }
  }

  object Solution2 {
    // There's a few ways to do this actually, you could immediately .map(_.toOption) inside the traversing function
    // They may talk about cats and EitherT or flatTraverse, since we don't have cats in scope, there isn't that
    // arguably more typed fp solution available, but points should be awarded if they discuss it

    def processRequests(payloads: List[Map[String, Int]]): Future[List[Response]] =
      Future
        .traverse(payloads.map(Request.apply[Int]))(DummyClient.sendRequest[Int])
        .map(_.flatMap(_.toOption))
  }

  object Solution3 {
    // To test 3 they will need to add this implementation of the Serde to test with `Map[String, String]`
    implicit val StringSerde: Serde[String] = new Serde[String] {
      override def deserialize(value: String): String = value
    }

    // obviously the changes here are small to get the code to run the examples successfully the real work is knowing to
    // write the instance of the Serde for String.
    def processRequests[T: Serde](payloads: List[Map[String, T]]): Future[List[Response]] = {
      Future
        .traverse(payloads.map(Request.apply[T]))(DummyClient.sendRequest[T])
        .map(_.flatMap(_.toOption))
    }
  }
}
