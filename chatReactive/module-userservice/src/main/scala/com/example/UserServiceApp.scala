package com.example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.server.Directives._

import scala.collection.mutable.Map
import akka.http.scaladsl.Http

object UserServiceApp {

  val usernames = Map[Int, String]() // userId => username

  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      implicit val system = context.system

      val route =
        concat(
          path("set_username") {
            concat(
              put {
                parameters("user_id", "username") { (id, username) =>
                  println(s"Set username for $id as $username")
                  usernames(id.toInt) = username
                  complete("done")
                }
              }
            )
          },
          path("get_username") {
            get {
              parameters("user_id") { (userId) =>
                println(s"Get username of ${userId}.")
                val username: String = usernames.get(userId.toInt).getOrElse("unknown")
                complete(username)
              }
            }
          }
        )

      Http().newServerAt("0.0.0.0", 8080).bind(route)
      Behaviors.empty
    }
  }

  def main(args: Array[String]): Unit = {
  
    val config = ConfigFactory.load()
    kamon.Kamon.init(config);

    // Create an Akka system
    ActorSystem[Nothing](RootBehavior(), "user-root", config)
  }

}
