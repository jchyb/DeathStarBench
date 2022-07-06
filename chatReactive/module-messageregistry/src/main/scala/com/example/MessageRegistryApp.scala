package com.example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.server.Directives._

import scala.collection.mutable.Map
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpMethods

object MessageRegistryApp {

  val messagesByRoom = Map[Int, List[(Int, String)]]() // roomId => (userId, message) 

  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      implicit val system = context.system
      val http = Http()

      val route =
        concat(
          path("send_message") {
            put {
              parameters("room_id", "user_id", "message") { (roomId, userId, message) =>
                println(s"Set message $message by $userId to $roomId.")
                val roomMessages =
                  messagesByRoom.get(roomId.toInt).map((userId.toInt, message) :: _)
                  .getOrElse(List((userId.toInt, message)))
                println(roomMessages)
                messagesByRoom(roomId.toInt) = roomMessages
                complete("ok")
              }
            }
          },
          path("get_messages") {
            get {
              parameters("room_id") { (roomId) =>
                val roomMessages = messagesByRoom(roomId.toInt)
                println(s"Getting messages from $roomId.")
                val messagesAsString = roomMessages.mkString("\n") // TODO to json ?
                // TODO include names
                complete(messagesAsString)
              }
            }
          },
        )

    val bindingFuture = http.newServerAt("0.0.0.0", 8080).bind(route)
      Behaviors.empty
    }
  }

  def main(args: Array[String]): Unit = {
  
    val config = ConfigFactory.load()
    kamon.Kamon.init(config);

    // Create an Akka system
    ActorSystem[Nothing](RootBehavior(), "messageregistry-root", config)
  }

}
