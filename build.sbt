import com.github.aalleexxeeii.hocon.sbt.HoconPlugin

scalaVersion := "2.11.7"

lazy val root = (project in file(".")).
  enablePlugins(HoconPlugin).
  settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % "2.4.3"
    ),
    hoconExtraResources += "common"
  )

