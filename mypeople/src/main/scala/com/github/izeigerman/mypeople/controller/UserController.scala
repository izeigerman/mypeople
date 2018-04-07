/*
 * Copyright 2018 Iaroslav Zeigerman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.izeigerman.mypeople.controller

import akka.actor.ActorRef
import akka.pattern.ask
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import akka.http.scaladsl.model._
import akka.util.Timeout
import com.github.izeigerman.mypeople.api._
import com.github.izeigerman.mypeople.service._

import scala.concurrent.ExecutionContext
import UserController._
import akka.http.scaladsl.marshalling.ToResponseMarshaller

class UserController(service: ActorRef)(implicit executionContext: ExecutionContext,
                                        timeout: Timeout)
  extends Directives with SprayJsonSupport with MessagesJsonProtocol {

  val route: Route =
    pathPrefix("users") {
      pathEndOrSingleSlash {
        parameter('page.as[Int].?) { page =>
          onSuccess(service ? GetUsers(page.getOrElse(1))) {
            case users: PaginatedUsers => respond(StatusCodes.OK -> users)
          }
        }
      } ~
      path("search") {
        parameters('query.?) { query =>
          onSuccess(service ? GetUsersByQuery(query.getOrElse(""))) {
            case users: PaginatedUsers => respond(StatusCodes.OK -> users)
          }
        }
      } ~
      path(IntNumber) { userId =>
        onSuccess(service ? GetUserById(userId)) {
          case user: UserWithRecommended => respond(StatusCodes.OK -> user)
          case UserNotFound => respond(StatusCodes.NotFound)
        }
      }
    }

    private def respond[T: ToResponseMarshaller](response: T) = {
      respondWithHeaders(ResponseHeaders) {
        complete(response)
      }
    }
}

object UserController {
  def apply(service: ActorRef)(implicit executionContext: ExecutionContext,
                               timeout: Timeout): UserController = {
    new UserController(service)
  }

  private val ResponseHeaders = List(
    headers.`Access-Control-Allow-Origin`.*
  )
}
