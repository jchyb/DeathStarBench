package gateway

import kamon.http4s.middleware.{server, client}
import org.http4s._
import org.http4s.ember.server.EmberServerBuilder
import cats.effect._
import org.http4s.dsl.io._

import com.comcast.ip4s._
import org.http4s.implicits._
import org.http4s.server.Router

import org.http4s.client.Client
import org.http4s.client.JavaNetClientBuilder
import com.typesafe.config.ConfigFactory

object GatewayApp extends IOApp {

  object UserIdQueryParamMatcher extends QueryParamDecoderMatcher[Int]("user_id")
  object RoomIdQueryParamMatcher extends QueryParamDecoderMatcher[Int]("room_id")
  object MessageQueryParamMatcher extends QueryParamDecoderMatcher[String]("message")
  object UsernameQueryParamMatcher extends QueryParamDecoderMatcher[String]("username")

  val userServiceIp = System.getenv("USER_SERVICE_IP")
  val messageRoomServiceIp = System.getenv("MESSAGE_ROOM_SERVICE_IP")
  
  def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigFactory.load()
    kamon.Kamon.init(config);
  
    val httpClient: Client[IO] = client.KamonSupport(JavaNetClientBuilder[IO].create)

    println(s"Found userServiceIp: $userServiceIp")
    println(s"Found messageRoomServiceIp: $messageRoomServiceIp")

    val service = HttpRoutes.of[IO] {
      case PUT -> Root / "set_username" :? UserIdQueryParamMatcher(userId) +& UsernameQueryParamMatcher(username) =>
        println(userId)
        println(username)
        Ok(httpClient.expect[String](Request[IO](Method.PUT, Uri.unsafeFromString(s"http://${userServiceIp}:8080/set_username?user_id=${userId}&username=$username"))))
      case PUT -> Root / "set_room" :? UserIdQueryParamMatcher(userId) +& RoomIdQueryParamMatcher(roomId) =>
        println(userId)
        println(roomId)
        Ok(httpClient.expect[String](Request[IO](Method.PUT, Uri.unsafeFromString(s"http://${messageRoomServiceIp}:8080/set_room?user_id=$userId&room_id=$roomId"))))
      case PUT -> Root / "send_message" :? UserIdQueryParamMatcher(userId) +& MessageQueryParamMatcher(message) =>
        println(userId)
        println(message)
        Ok(httpClient.fetchAs[String](Request[IO](Method.PUT, Uri.unsafeFromString(s"http://${messageRoomServiceIp}:8080/send_message?user_id=$userId&message=$message"))))
      case GET -> Root / "get_messages" :? UserIdQueryParamMatcher(userId) =>
        println(userId)
        Ok(httpClient.expect[String](s"http://${messageRoomServiceIp}:8080/get_messages?user_id=$userId"))
      case GET -> Root / "hello" =>
        Ok("hello back")
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