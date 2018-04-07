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
package com.github.izeigerman.mypeople.service

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props, Stash}
import akka.pattern.pipe
import com.github.izeigerman.mypeople.api.{PaginatedUsers, RecommendedUser, User, UserWithRecommended}
import com.github.izeigerman.mypeople.source.DataSource

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import UserService._
import breeze.linalg.StorageVector
import com.github.izeigerman.mypeople.recommender._

import scala.util.Failure

class UserService(dataSource: DataSource,
                  estimator: Estimator) extends Actor with ActorLogging with Stash {

  implicit val executionContext: ExecutionContext = context.dispatcher

  private val config = context.system.settings.config.getConfig("mypeople")

  private val maxTopSimilarUsers = config.getInt("max-top-similar-users")
  private val dataRefreshInterval = config.getDuration("data-refresh-interval", TimeUnit.MILLISECONDS)

  private var users: Map[Int, User] = Map.empty
  private var userVectors: Map[Int, StorageVector[Double]] = Map.empty
  private var isInitialized: Boolean = false

  override def receive: Receive = uninitializedReceive

  override def preStart(): Unit = {
    self ! FetchUsers
  }

  private def uninitializedReceive: Receive = fetchUsersReceive orElse {
    case _ => stash()
  }

  private def initializedReceive: Receive = fetchUsersReceive orElse clientMessagesReceive

  private def fetchUsersReceive: Receive = {
    case FetchUsers =>
      log.info(s"Fetching users...")
      dataSource.findAllUsers.pipeTo(self)

    case PaginatedUsers(users, _) =>
      log.info(s"Fetched ${users.size} users")
      this.users = users.map(u => (u.id, u)).toMap
      this.userVectors = Vectorizer.vectorize(this.users)
      if (!isInitialized) initService()

    case Failure(e) =>
      log.error(s"Failed to fetch users: ${e.getMessage}")
  }

  private def clientMessagesReceive: Receive = {
    case GetUsers(page) => dataSource.findUsers(page).pipeTo(sender())
    case GetUsersByQuery(query) => dataSource.findUsersByQuery(query).pipeTo(sender())
    case GetUserById(id) =>
      if (users.contains(id)) {
        val targetUser = users(id)
        sender() ! UserWithRecommended(targetUser, recommendUsers(id).take(maxTopSimilarUsers))
      } else {
        sender() ! UserNotFound
      }
  }

  private def recommendUsers(userId: Int): Seq[RecommendedUser] = {
    val targetVector = userVectors(userId)
    val scores = estimator.scoreLikeness(targetVector, userVectors)
    scores.toIndexedSeq
      .sortBy(-_._2)
      .withFilter(i => i._1 != userId)
      .map {
        case (id, score) => RecommendedUser(users(id), (score * 10000.0).toInt.toDouble / 10000.0)
      }
  }

  private def initService(): Unit = {
    context.become(initializedReceive)
    val interval = FiniteDuration(dataRefreshInterval, TimeUnit.MILLISECONDS)
    context.system.scheduler.schedule(interval, interval, self, FetchUsers)
    isInitialized = true
    unstashAll()
  }
}

object UserService {
  case object FetchUsers

  def apply(source: DataSource, estimator: Estimator)(implicit factory: ActorRefFactory): ActorRef = {
    factory.actorOf(Props(classOf[UserService], source, estimator), "userService")
  }
}
