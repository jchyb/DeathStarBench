package userservice

import kamon.http4s.middleware.{server, client}
import org.http4s.ember.server.EmberServerBuilder
import cats.effect._
import org.http4s.dsl.io._

import cats.syntax.all._
import com.comcast.ip4s._
import org.http4s.implicits._
import org.http4s.server.Router

import org.http4s.client.Client
import org.http4s.client.JavaNetClientBuilder
import cats.effect.unsafe.implicits.global
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Method
import org.http4s.Uri
import com.typesafe.config.ConfigFactory

object MessageRoomApp extends IOApp {

  val roomIdByUser = scala.collection.mutable.Map[Int, Int]()

  object UserIdQueryParamMatcher extends QueryParamDecoderMatcher[Int]("user_id")
  object RoomIdQueryParamMatcher extends QueryParamDecoderMatcher[Int]("room_id")
  object MessageQueryParamMatcher extends QueryParamDecoderMatcher[String]("message")
  object UsernameQueryParamMatcher extends QueryParamDecoderMatcher[String]("username")

  val userServiceIp = System.getenv("USER_SERVICE_IP")
  val messageRegistryServiceIp = System.getenv("MESSAGE_REGISTRY_SERVICE_IP")
  
  def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigFactory.load()
    kamon.Kamon.init(config)

    val httpClient: Client[IO] = client.KamonSupport(JavaNetClientBuilder[IO].create)

    println(s"Found userServiceIp: $userServiceIp")
    println(s"Found messageRegistryServiceIp: $messageRegistryServiceIp")

    val service = HttpRoutes.of[IO] {
      case PUT -> Root / "set_room" :? UserIdQueryParamMatcher(userId) +& RoomIdQueryParamMatcher(roomId) =>
        println(userId)
        println(roomId)
        roomIdByUser(userId.toInt) = roomId.toInt
        Ok("done")
      case PUT -> Root / "send_message" :? UserIdQueryParamMatcher(userId) +& MessageQueryParamMatcher(message) =>
        val roomId = roomIdByUser(userId)
        println(s"Sending $message of user $userId to room $roomId")
        Ok(httpClient.expect[String](Request[IO](Method.PUT, Uri.unsafeFromString(s"http://${messageRegistryServiceIp}:8080/send_message?room_id=$roomId&user_id=$userId&message=$message"))))
      case GET -> Root / "get_messages" :? UserIdQueryParamMatcher(userId) =>
        println(s"Getting messages for $userId")
        val roomId = roomIdByUser(userId)
        val str = httpClient.expect[String](s"http://${messageRegistryServiceIp}:8080/get_messages?room_id=$roomId")
        val formattedStr = str.unsafeRunSync().split("\n").map { msg =>
          println(msg)
          val arr = msg.split(':')
          val (userId, msgStr) = (arr.head, arr.tail)
          s"user:${msgStr.mkString("")}"
        }.mkString("\n")
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