package gateway

import org.http4s._
import org.http4s.ember.server.EmberServerBuilder
import cats.effect._
import org.http4s.dsl.io._

import com.comcast.ip4s._
import org.http4s.implicits._
import org.http4s.server.Router
import kamon.http4s.middleware.server
import com.typesafe.config.ConfigFactory

object MessageRegistryApp extends IOApp {

  case class Message(uid: Int, message: String)
  val messagesByRoom = scala.collection.mutable.Map[Int, List[Message]]() // roomId => messages

  object UserIdQueryParamMatcher extends QueryParamDecoderMatcher[Int]("user_id")
  object RoomIdQueryParamMatcher extends QueryParamDecoderMatcher[Int]("room_id")
  object MessageQueryParamMatcher extends QueryParamDecoderMatcher[String]("message")
  object UsernameQueryParamMatcher extends QueryParamDecoderMatcher[String]("username")

  def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigFactory.load()
    kamon.Kamon.init(config);

    val service = HttpRoutes.of[IO] {
      case PUT -> Root / "send_message" :? UserIdQueryParamMatcher(userId) +& RoomIdQueryParamMatcher(roomId) +& MessageQueryParamMatcher(message) =>
        println(s"Set message $message by $userId to $roomId.")
        val roomMessages =
          messagesByRoom.get(roomId.toInt).map(Message(userId.toInt, message) :: _)
          .getOrElse(List(Message(userId.toInt, message)))
        println(roomMessages)
        messagesByRoom(roomId.toInt) = roomMessages
        Ok("done")
      case GET -> Root / "get_messages" :? RoomIdQueryParamMatcher(roomId) =>
        println(s"Getting messages from $roomId.")
        val roomMessages = messagesByRoom.get(roomId.toInt).getOrElse(List(Message(0, "dummy message")))
        val formattedStr = roomMessages.map( msg => 
            s"${msg.uid}: ${msg.message}"
        ).mkString("\n")
        Ok(formattedStr)
    }

    val httpApp = Router("/" -> server.KamonSupport(service, "", 0)).orNotFound

    EmberServerBuilder.default[IO]
      .withPort(port"8080")
      .withHost(host"0.0.0.0")
      .withHttpApp(httpApp)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}