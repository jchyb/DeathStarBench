/*
 * Copyright (C) 2020-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import spray.json.DefaultJsonProtocol._
import akka.actor.typed.Behavior

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.io.StdIn
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpMethods

object HttpServerWithActorInteraction {

  object Auction {

    sealed trait Message
    case class Bid(userId: String, offer: Int) extends Message
    case class GetBids(replyTo: ActorRef[Bids]) extends Message
    case class Bids(bids: List[Bid])

    def apply(): Behaviors.Receive[Message] = apply(List.empty)

    def apply(bids: List[Bid]): Behaviors.Receive[Message] = Behaviors.receive {
      case (ctx, bid @ Bid(userId, offer)) =>
        ctx.log.info(s"Bid complete: $userId, $offer")
        apply(bids :+ bid)
      case (_, GetBids(replyTo)) =>
        replyTo ! Bids(bids)
        Behaviors.same
    }

  }

  // these are from spray-json
  implicit val bidFormat = jsonFormat2(Auction.Bid)
  implicit val bidsFormat = jsonFormat1(Auction.Bids)

  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    val config = ConfigFactory.load()
    val auction: ActorRef[Auction.Message] = context.spawn(Auction(), "auction")
    // needed for the future flatMap/onComplete in the end
    implicit val system = context.system
    implicit val executionContext: ExecutionContext = context.executionContext
    import Auction._  // this was from an example (TODO remove)
    // println(config.values)
    println("ok")
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
              // spawn actor making the request to another microservice, wait for it and return
              // val actor = context.spawn("Handler", hande)
              // actor ? 
              // TODO move to actor
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

    val bindingFuture = Http().newServerAt("0.0.0.0", 8080).bind(route)
    println(s"Server online at http://localhost:8080/")
    // StdIn.readLine() // let it run until user presses return
    // println("Derver end???")
    // bindingFuture
    //   .flatMap(_.unbind()) // trigger unbinding from the port
    //   .onComplete(_ => system.terminate()) // and shutdown when done
    Behaviors.empty
  }
}