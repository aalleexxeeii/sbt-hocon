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

licenses += "Apache-2.0" → url("https://www.apache.org/licenses/LICENSE-2.0.html")

publishMavenStyle := true

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
  "com.github.scopt" %% "scopt" % "3.5.0"
)

developers := List(Developer("alexei", "Alexei", "alexeithedeveloper@gmail.com", url("https://github.com/aalleexxeeii/")))

resolvers += Resolver.sonatypeRepo("public")

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ ⇒ false }
