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

// Backend dependency versions.
val AkkaHttpVersion = "10.0.11"
val BreezeVersion = "0.13.2"
val SprayJsonVersion = "1.3.3"
val ScalaTestVersion = "3.0.5"
val ScalamockVersion = "3.4.2"
val Slf4jVersion = "1.7.19"

// UI dependency version.
val ScalaJSDomVersion = "0.9.1"
val ScalaTagsVersion = "0.6.2"

val CommonSettings = Seq(
  organization := "com.github.izeigerman",
  scalaVersion := "2.12.2",
  version := "0.0.1-SNAPSHOT",

  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-language:higherKinds"),

  parallelExecution in Test := false,

  test in assembly := {}
)

val MyPeopleSettings = CommonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-core" % AkkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
    "org.scalanlp" %% "breeze" % BreezeVersion,
    "io.spray" %% "spray-json" % SprayJsonVersion,
    "org.slf4j" % "slf4j-api" % Slf4jVersion,
    "org.slf4j" % "slf4j-log4j12" % Slf4jVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % "test->*",
    "org.scalatest" %% "scalatest" % ScalaTestVersion % "test->*",
    "org.scalamock" %% "scalamock-scalatest-support" % ScalamockVersion % "test->*"
  ),

  mainClass in (Compile, run) := Some("com.github.izeigerman.mypeople.MyPeopleMain"),

  assemblyMergeStrategy in assembly := {
    case "log4j.properties" => MergeStrategy.concat
    case "application.conf" => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

val MyPeopleUiSettings = CommonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % ScalaJSDomVersion,
    "com.lihaoyi" %%% "scalatags" % ScalaTagsVersion
  )
)

val NoPublishSettings = CommonSettings ++ Seq(
  publishArtifact := false,
  publish := {}
)

lazy val mypeopleRoot = (project in file("."))
    .settings(NoPublishSettings: _*)
    .aggregate(mypeople, mypeopleUi)
    .disablePlugins(sbtassembly.AssemblyPlugin)

lazy val mypeople = (project in file("mypeople"))
    .settings(MyPeopleSettings: _*)

lazy val mypeopleUi = (project in file("mypeople-ui"))
    .settings(MyPeopleUiSettings: _*)
    .enablePlugins(ScalaJSPlugin)
    .enablePlugins(WorkbenchPlugin)
    .disablePlugins(sbtassembly.AssemblyPlugin)
