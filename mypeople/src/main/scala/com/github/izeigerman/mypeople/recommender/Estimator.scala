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

trait Estimator {
  def scoreLikeness[K](target: StorageVector[Double],
                       vectors: Map[K, StorageVector[Double]]): Map[K, Double]
}

object Estimator {

  /**
    * The estimator that uses cosinus of the angle between two vectors as a score of similarity.
    */
  object CosEstimator extends Estimator {
    override def scoreLikeness[K](target: StorageVector[Double],
                                  vectors: Map[K, StorageVector[Double]]): Map[K, Double] = {
      vectors.map {
        case (id, vector) =>
          id -> cos(target, vector)
      }
    }

    private def cos(vector1: StorageVector[Double],
                    vector2: StorageVector[Double]): Double = {
      sum(vector1 * vector2)
    }
  }
}
