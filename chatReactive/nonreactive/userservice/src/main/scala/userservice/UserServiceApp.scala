package messageregistry

import org.http4s._
import org.http4s.ember.server.EmberServerBuilder
import cats.effect._
import org.http4s.dsl.io._

import com.comcast.ip4s._
import org.http4s.server.Router
import kamon.http4s.middleware.server
import com.typesafe.config.ConfigFactory

object UserServiceApp extends IOApp {

  object UserIdQueryParamMatcher extends QueryParamDecoderMatcher[Int]("user_id")
  object UsernameQueryParamMatcher extends QueryParamDecoderMatcher[String]("username")

  val usernames = scala.collection.mutable.Map[Int, String]() // userId => username
  
  def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigFactory.load()
    kamon.Kamon.init(config)

    val service = HttpRoutes.of[IO] {
      case PUT -> Root / "set_username" :? UserIdQueryParamMatcher(userId) +& UsernameQueryParamMatcher(username) =>
        usernames(userId) = username
        println(s"Setting user $userId username to $username")
        Ok("done")
      case PUT -> Root / "get_username" :? UserIdQueryParamMatcher(userId)=>
        Ok(usernames(userId))
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