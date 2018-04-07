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

import com.github.izeigerman.mypeople.api.{User, PaginatedUsers}

import scala.concurrent.Future

trait DataSource {

  def findAllUsers: Future[PaginatedUsers]

  def findUsers(page: Int): Future[PaginatedUsers]

  def findUsersByQuery(query: String): Future[PaginatedUsers]

  def findUserById(id: Int): Future[User]
}
