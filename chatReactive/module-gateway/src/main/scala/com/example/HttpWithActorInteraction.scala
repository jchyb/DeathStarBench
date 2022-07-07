/*
 * Copyright (C) 2020-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example

import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.actor.typed.Behavior

import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpMethods

object HttpServerWithActorInteraction {

  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    val config = ConfigFactory.load()
    implicit val system = context.system
    val userServiceIp = config.getString("microservices.user-service-ip")
    val messageRoomServiceIp = config.getString("microservices.messageroom-service-ip")
    println(s"DEBUG: userservice ip: $userServiceIp")
    println(s"DEBUG: messageroomservice ip: $messageRoomServiceIp")

    val http = Http(system)

    val route =
      concat(
        path("set_username") {
          put {
            parameters("user_id", "username") { (id, username) =>
              println(s"Called 'set_username' with $id and $username")
              val response = http.singleRequest(
                HttpRequest(uri = s"http://${userServiceIp}:8080/set_username?user_id=$id&username=$username", method = HttpMethods.PUT)
              )
              complete(response)
            }
          }
        },
        path("set_room") {
          put {
            parameters("user_id", "room_id") { (userId, roomId) =>
              val response = http.singleRequest(
                HttpRequest(uri = s"http://${messageRoomServiceIp}:8080/set_room?user_id=$userId&room_id=$roomId", method = HttpMethods.PUT)
              )
              complete(response)
            }
          }
        },
        path("send_message") {
          put {
            parameters("user_id", "message") { (userId, message) =>
              val response = http.singleRequest(
                HttpRequest(uri = s"http://${messageRoomServiceIp}:8080/send_message?user_id=$userId&message=$message", method = HttpMethods.PUT)
              )
              complete(response)
            }
          }
        },
        path("get_messages") {
          get {
            parameters("user_id") { (userId) =>
              val response = http.singleRequest(
                HttpRequest(uri = s"http://${messageRoomServiceIp}:8080/get_messages?user_id=$userId", method = HttpMethods.GET)
              )
              complete(response)
            }
          }
        }
      )

    Http().newServerAt("0.0.0.0", 8080).bind(route)
    println(s"Server online at http://localhost:8080/")
    Behaviors.empty
  }
}