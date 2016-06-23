organization := "com.github.aalleexxeeii"

name := "sbt-hocon"

description := "sbt plugin with HOCON utilities"

sbtPlugin := true

homepage := Some(url("https://github.com/aalleexxeeii/sbt-hocon"))

scmInfo := Some(
  ScmInfo(
    browseUrl = url("https://github.com/aalleexxeeii/sbt-hocon"),
    connection = "https://github.com/aalleexxeeii/sbt-hocon.git"
  )
)

libraryDependencies += "com.typesafe" % "config" % "1.3.0"
