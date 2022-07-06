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

object MessageRoomApp {

  val roomIdByUser = Map[Int, Int]() // userId => roomId 

  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      val config = ConfigFactory.load()
      implicit val system = context.system
      val http = Http()
      val messageRegistryServiceIp = config.getString("microservices.messageregistry-service-ip")
      println(s"DEBUG: messageRegistryServiceIp: $messageRegistryServiceIp")

      val route =
        concat(
          path("set_room") {
            put {
              parameters("user_id", "room_id") { (userId, roomId) =>
                println(s"Set room for user: $userId as room: $roomId")
                //spawn actor making the request, wait for it and return
                roomIdByUser(userId.toInt) = roomId.toInt
                complete("done")
              }
            }
          },
          path("send_message") {
            put {
              parameters("user_id", "message") { (userId, message) =>
                val roomId = roomIdByUser.get(userId.toInt).getOrElse(0)
                println(s"Sending message $message by userId to $roomId.")
                val response = http.singleRequest(
                  HttpRequest(uri = s"http://${messageRegistryServiceIp}:8080/send_message?room_id=$roomId&user_id=$userId&message=$message", method = HttpMethods.PUT)
                )
                complete(response)
              }
            }
          },
          path("get_messages") {
            get {
              parameters("user_id") { (userId) =>
                val roomId = roomIdByUser.get(userId.toInt).getOrElse(0)
                println(s"Getting messages for $userId by from $roomId.")
                val response = http.singleRequest(
                  HttpRequest(uri = s"http://${messageRegistryServiceIp}:8080/get_messages?room_id=$roomId", method = HttpMethods.GET)
                )
                complete(response)
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
    ActorSystem[Nothing](RootBehavior(), "messageroom-root", config)
  }

}
