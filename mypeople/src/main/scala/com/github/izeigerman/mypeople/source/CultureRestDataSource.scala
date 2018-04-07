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
package com.github.izeigerman.mypeople.source

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.GenericHttpCredentials
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.github.izeigerman.mypeople.api._
import spray.json._
import CultureRestDataSource._

import scala.concurrent.{ExecutionContext, Future}

class CultureRestDataSource(baseUri: String, token: String)(implicit system: ActorSystem,
                                                            executionContext: ExecutionContext)
  extends DataSource with MessagesJsonProtocol {

  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private val http: HttpExt = Http()

  override final def findAllUsers: Future[PaginatedUsers] = {
    val firstPage = findUsers(1)
    val allPages: Future[Seq[PaginatedUsers]] = firstPage
      .flatMap(first => first.pagination match {
        case Some(page) =>
          val totalPages = page.totalPages
          val groupSize = math.ceil(totalPages.toDouble / MaxParallelRequests.toDouble).toInt
          val grouped = (2 to totalPages).grouped(groupSize)
          val groupedFutures = grouped.map(i => fetchUsers(i.head, i.last)).toSeq
          Future.sequence(Future.successful(first) +: groupedFutures)
        case None =>
          Future.successful(Seq(first))
      })
    allPages
      .map(users => users.reduce((left, right) => PaginatedUsers(left.users ++ right.users, None)))
  }

  override final def findUsers(page: Int): Future[PaginatedUsers] = {
    require(page > 0)
    toEntity[PaginatedUsers](http.singleRequest(buildHttpRequest(s"users?page=$page")))
  }

  override final def findUsersByQuery(query: String): Future[PaginatedUsers] = {
    val encodedQuery = URLEncoder.encode(query)
    toEntity[PaginatedUsers](http.singleRequest(buildHttpRequest(s"autocomplete/users?query=$encodedQuery")))
  }

  override final def findUserById(id: Int): Future[User] = {
    toEntity[WrappedUser](http.singleRequest(buildHttpRequest(s"users/$id"))).map(_.user)
  }

  private def fetchUsers(pageStart: Int, pageEnd: Int): Future[PaginatedUsers] = {
    val start = findUsers(pageStart)
    (pageStart + 1 to pageEnd).foldLeft(start)((users, pageIdx) => {
      users.flatMap(u1 => findUsers(pageIdx).map(u2 => PaginatedUsers(u1.users ++ u2.users, None)))
    })
  }

  private def buildHttpRequest(path: String): HttpRequest = {
    val authorizationHeader = headers.Authorization(GenericHttpCredentials("token", token))
    val userAgent = headers.`User-Agent`(UserAgent)
    val uri = s"$baseUri/$path"
    HttpRequest(uri = uri, headers = List(authorizationHeader, userAgent))
  }

  private def toEntity[T: JsonReader](future: Future[HttpResponse]): Future[T] = {
    future
      .flatMap(r => {
        if (r.status.isFailure()) {
          r.discardEntityBytes()
          Future.failed(DataSourceException(r.status.reason()))
        } else {
          r.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
        }
      })
      .map(data => data.decodeString("UTF-8").parseJson.convertTo[T])
  }
}

object CultureRestDataSource {
  val MaxParallelRequests = 8
  val UserAgent = "Chrome/64.0.3282.186"
}
