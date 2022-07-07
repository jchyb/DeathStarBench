package com.example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.server.Directives._

import scala.collection.mutable.Map
import akka.http.scaladsl.Http
import akka.stream.scaladsl.Source
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.util.ByteString
import akka.stream.scaladsl.Flow

object MessageRegistryApp {

  case class Message(uid: Int, message: String)
  val messagesByRoom = Map[Int, List[Message]]() // roomId => messages

  object MessageJsonProtocol
    extends akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
    with spray.json.DefaultJsonProtocol {

    implicit val messageFormat = jsonFormat2(Message.apply)
  }

  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      import MessageJsonProtocol._
      implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
        EntityStreamingSupport.json()
        .withFramingRenderer(Flow[ByteString].map(bs => bs ++ ByteString("\n")))
      implicit val system = context.system
      val http = Http()

      val route =
        concat(
          path("send_message") {
            put {
              parameters("room_id", "user_id", "message") { (roomId, userId, message) =>
                println(s"Set message $message by $userId to $roomId.")
                val roomMessages =
                  messagesByRoom.get(roomId.toInt).map(Message(userId.toInt, message) :: _)
                  .getOrElse(List(Message(userId.toInt, message)))
                println(roomMessages)
                messagesByRoom(roomId.toInt) = roomMessages
                complete("done")
              }
            }
          },
          path("get_messages") {
            get {
              parameters("room_id") { (roomId) =>
                val roomMessages = messagesByRoom.get(roomId.toInt).getOrElse(List(Message(0, "dummy message")))
                println(s"Getting messages from $roomId.")
                val source = Source(roomMessages) // Reactive stream implementation - server side
                complete(source)
              }
            }
          },
        )

      http.newServerAt("0.0.0.0", 8080).bind(route)
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
