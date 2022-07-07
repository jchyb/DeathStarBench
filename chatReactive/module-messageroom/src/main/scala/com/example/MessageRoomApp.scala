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
import akka.stream.scaladsl.Flow
import scala.concurrent.Future
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.util.ByteString
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.marshalling.Marshalling

object MessageRoomApp {

  case class Message(uid: Int, message: String)
  implicit val messageFormat = jsonFormat2(Message)
  val roomIdByUser = Map[Int, Int]() // userId => roomId 
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
    EntityStreamingSupport.json()
    .withFramingRenderer(Flow[ByteString].map(bs => bs ++ ByteString("\n")))

  object RootBehavior {
  
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      val config = ConfigFactory.load()
      implicit val system = context.system
      implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
      val http = Http()
      val messageRegistryServiceIp = config.getString("microservices.messageregistry-service-ip")
      val userServiceIp = config.getString("microservices.user-service-ip")
      println(s"DEBUG: messageRegistryServiceIp: $messageRegistryServiceIp")

      implicit val stringFormat = Marshaller[String, ByteString] { ec => s =>
          Future.successful {
            List(Marshalling.WithFixedContentType(ContentTypes.`application/json`, () =>
              ByteString("\"" + s + "\"")) // String in a JSON format
            )
          }
        }

      val route =
        concat(
          path("set_room") {
            put {
              parameters("user_id", "room_id") { (userId, roomId) =>
                println(s"Set room for user: $userId as room: $roomId")
                // In a real system we would probably write to the database,
                // or use a cluster for in-app caching
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
                // Reactive stream implementation - client side
                val src = response.map { 
                  _.entity.dataBytes
                    .via(jsonStreamingSupport.framingDecoder)
                    .mapAsync(1)(bytes => Unmarshal(bytes).to[Message].map { message =>
                        // Query and inclusion of username
                        http.singleRequest(
                          HttpRequest(uri = s"http://${userServiceIp}:8080/get_username?user_id=$userId", method = HttpMethods.GET)
                        ).map { usernameResponse =>
                          Unmarshal(usernameResponse.entity).to[String].map { username =>
                            println("username:" + username)
                            s"${username}: ${message.message}"
                          }
                        }
                    })
                }
                complete(src)
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
    ActorSystem[Nothing](RootBehavior(), "messageroom-root", config)
  }

}
