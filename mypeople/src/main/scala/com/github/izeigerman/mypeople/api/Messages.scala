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
package com.github.izeigerman.mypeople.api

case class PageInfo(currentPage: Int, totalPages: Int, totalCount: Int)

case class Interest(id: Int, name: String)

case class Department(id: Int, name: String)

case class Location(id: Int, name: String)

case class Avatar(thumbUrl: String, mediumUrl: String, largeUrl: String, originalUrl: String)

case class User(id: Int, name: String, email: String, title: Option[String], location: Location,
                points: Int, departments: Seq[Department], interests: Seq[Interest], avatar: Avatar)

case class WrappedUser(user: User)

case class PaginatedUsers(users: Seq[User], pagination: Option[PageInfo])

case class RecommendedUser(user: User, score: Double)

case class UserWithRecommended(user: User, recommended: Seq[RecommendedUser])

trait MessagesJsonProtocol extends SnakifiedSprayJsonSupport {
  implicit val paginationJsonProtocol = jsonFormat3(PageInfo.apply)
  implicit val interestJsonProtocol = jsonFormat2(Interest.apply)
  implicit val departmentJsonProtocol = jsonFormat2(Department.apply)
  implicit val locationJsonProtocol = jsonFormat2(Location.apply)
  implicit val avatarJsonProtocol = jsonFormat4(Avatar.apply)
  implicit val userJsonProtocol = jsonFormat9(User.apply)
  implicit val usersJsonProtocol = jsonFormat2(PaginatedUsers.apply)
  implicit val wrappedUserJsonProtocol = jsonFormat1(WrappedUser.apply)
  implicit val recommendedUserJsonProtocol = jsonFormat2(RecommendedUser.apply)
  implicit val detailedUserJsonProtocol = jsonFormat2(UserWithRecommended.apply)
}
