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
package com.github.izeigerman.mypeople

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.server.PathMatchers
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.github.izeigerman.mypeople.controller.UserController
import com.github.izeigerman.mypeople.recommender._
import com.github.izeigerman.mypeople.service.UserService
import com.github.izeigerman.mypeople.source.CultureRestDataSource
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Failure

object MyPeopleMain extends App {
  val config = ConfigFactory.load()
  implicit val actorSystem = ActorSystem("MyPeopleSystem", config)
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = actorSystem.dispatcher
  implicit val clientTimeout = Timeout(
    config.getDuration("mypeople.client-request-timeout", TimeUnit.MILLISECONDS),
    TimeUnit.MILLISECONDS)

  val token = sys.env.getOrElse("CULTURE_TOKEN", "")
  val source = new CultureRestDataSource("https://api.<insert_host_here>", token)
  val userService = UserService(source, Estimator.CosEstimator)
  val userController = UserController(userService)

  val route = pathPrefix(PathMatchers.separateOnSlashes("api/1")) {
    userController.route
  }

  Http().bindAndHandle(route, "0.0.0.0", config.getInt("mypeople.api-port")).onComplete {
    case Failure(_) =>
      actorSystem.terminate()
    case _ =>
  }

  Await.result(actorSystem.whenTerminated, Duration.Inf)
}

