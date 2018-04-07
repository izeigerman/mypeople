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
package com.github.izeigerman.mypeople.recommender

import breeze.linalg._
import breeze.numerics._
import com.github.izeigerman.mypeople.api.User

trait Vectorizer[T] {
  def vectorize[K](values: Map[K, T]): Map[K, StorageVector[Double]]
}

object Vectorizer {

  def vectorize[K, T](values: Map[K, T])(implicit v: Vectorizer[T]): Map[K, StorageVector[Double]] = {
    v.vectorize(values)
  }

  /**
    * Creates user vectors based on their interests and location.
    */
  implicit val userVectorizer = new Vectorizer[User] {
    private val InterestsWeight = 1.0d
    private val DepartmentWeight = 0.5d
    private val LocationWeight = 0.8d

    override def vectorize[K](users: Map[K, User]): Map[K, StorageVector[Double]] = {
      val userValues = users.values

      val interests: Set[(String, Double)] = userValues
        .flatMap(_.interests.map(i => normalizeText(i.name) -> InterestsWeight)).toSet
      val departments: Set[(String, Double)] = userValues
        .flatMap(_.departments.map(d => d.name -> DepartmentWeight)).toSet
      val locations: Set[(String, Double)] = userValues
        .map(u => u.location.name -> LocationWeight).toSet

      val valuesWithWeightsAndIndex = (interests ++ departments ++ locations).toArray
        .sorted.zipWithIndex.toMap

      val valueToIndex: Map[String, Int] = valuesWithWeightsAndIndex.map {
        case ((value, _), idx) => value -> idx
      }
      val indexToWeight: Map[Int, Double] = valuesWithWeightsAndIndex.map {
        case ((_, weight), idx) => idx -> weight
      }

      val vectorLength = valuesWithWeightsAndIndex.size

      users.map {
        case (id, user) =>
          val userInterests = user.interests.map(i => normalizeText(i.name))
          val userDepartments = user.departments.map(_.name)
          val userLocation = user.location.name

          val vectorIdxs = (userInterests ++ userDepartments :+ userLocation).map(valueToIndex(_))
          val userAttributesNum = vectorIdxs.length.toDouble
          // Adjust weights according to a total number of attributes.
          // Make sure that users with a lower total number of attributes end up with
          // higher weights for those attributes.
          val userWeights = vectorIdxs.map(i => (i -> indexToWeight(i) * (1.0d / userAttributesNum)))

          val vector = SparseVector(vectorLength)(userWeights: _*)
          id -> normalizeVector(vector)
      }
    }
  }

  private def normalizeText(value: String): String = {
    // Attempt to strip a plural form.
    if (value.endsWith("s") || value.endsWith("e")) {
      value.substring(0, value.length - 1)
    } else if (value.endsWith("es")) {
      value.substring(0, value.length - 2)
    } else {
      value
    }
  }

  private def normalizeVector(vector: SparseVector[Double]): SparseVector[Double] = {
    val vectorNorm = sqrt(sum(pow(vector, 2)))
    vector / vectorNorm
  }
}
