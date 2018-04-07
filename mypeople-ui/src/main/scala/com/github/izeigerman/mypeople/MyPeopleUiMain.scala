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

import org.scalajs.dom
import dom.html
import org.scalajs.dom.ext.Ajax
import scalatags.JsDom.all._

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.URIUtils

object MyPeopleUiMain {

  val host: String = "localhost"
  val baseUri: String = s"http://$host:8080"

  @JSExportTopLevel("main")
  def main(target: html.Div): Unit = {
    val nameBox = input(
      `type`:="text",
      `class`:="nameBox",
      placeholder:="Enter the name"
    ).render

    val output = div(
      overflowY:="scroll",
      verticalAlign.middle
    ).render

    nameBox.onkeyup = (e: dom.Event) => {
      if (nameBox.value.nonEmpty) {
        findUsers(nameBox.value, output)
      }
    }

    target.appendChild(
      div(
        div(`class`:="block", h1("MyPeople", `class`:="page-header")),
        div(`class`:="block", nameBox),
        div(`class`:="block", output)
      ).render
    )
  }

  def findUsers(query: String, target: html.Div): Unit = {
    Ajax.get(buildUri(s"users/search?query=$query")).onComplete {
      case Success(xhr) =>
        val result = js.JSON.parse(xhr.responseText)
        renderUserListResponse(target, result)

      case _ =>
    }
  }

  def findUser(id: String, target: html.Div): Unit = {
    Ajax.get(buildUri(s"users/$id")).onComplete {
      case Success(xhr) =>
        val result = js.JSON.parse(xhr.responseText)
        renderUserRecommendationsResponse(target, result)

      case _ =>
    }
  }

  def renderUserRecommendationsResponse(target: html.Div, result: js.Dynamic): Unit = {
    target.innerHTML = ""

    val user = result.user
    val userTable = table(`class`:="table").render
    renderUserTableHeader(userTable, true)
    renderUser(target, userTable, user, Some("1.0"))

    val recommended = result.recommended.asInstanceOf[js.Array[js.Dynamic]]
    recommended.foreach(r => {
      val score = r.score.toString
      val user = r.user
      renderUser(target, userTable, user, Some(score))
    })

    target.appendChild(userTable)
  }

  def renderUserListResponse(target: html.Div, result: js.Dynamic): Unit = {
    val users = result.users.asInstanceOf[js.Array[js.Dynamic]]

    target.innerHTML = ""
    val userTable = table(`class`:="table").render
    renderUserTableHeader(userTable, false)
    users.foreach(u => renderUser(target, userTable, u, None))
    target.appendChild(userTable)
  }

  def renderUserTableHeader(table: html.Table, hasScore: Boolean): Unit = {
    val columns = List(
      td(),
      td(b("Name")),
      td(b("Location")),
      td(b("Departments")),
      td(b("Interests")),
      td(b("Profile Link"))
    )
    val updatedColumns = if (hasScore) columns :+ td(b("Similarity Score")) else columns
    val row = tr(updatedColumns: _*).render
    table.appendChild(row)
  }

  def renderUser(target: html.Div, table: html.Table, user: js.Dynamic, score: Option[String]): Unit = {
    val id = user.id
    val profileLink = s"https://platform.<insert_host_here>/people/$id"
    val name = user.name
    val location = user.location.name
    val image = user.avatar.thumb_url
    val interests = user.interests.asInstanceOf[js.Array[js.Dynamic]].map(i => i.name.toString)
    val departments = user.departments.asInstanceOf[js.Array[js.Dynamic]].map(d => d.name.toString)

    val columns = Array(
      td(img(src:=image, `class`:="img-circle")),
      td(s"$name"),
      td(s"$location"),
      td(
        ul(departments.map(li(_)): _*)
      ),
      td(
        ul(interests.map(li(_)): _*)
      ),
      td(a(href:=profileLink)("Profile"))
    )

    val updatedColumns = score.map(s => columns :+ td(s)).getOrElse(columns)

    val onClick = (e: dom.Event) => {
      findUser(id.toString, target)
    }

    val renderedColumns = updatedColumns
      .zipWithIndex.map {
        case (item, idx) =>
          val rendered = item.render
          if (idx != 5) {
            // Profile link block should not react on clicks.
            rendered.onclick = onClick
          }
          rendered
      }

    val row = tr.render
    renderedColumns.foreach(row.appendChild(_))

    table.appendChild(row)
  }

  def buildUri(path: String): String = {
    URIUtils.encodeURI(s"$baseUri/api/1/$path")
  }
}
