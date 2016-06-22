package com.github.aalleexxeeii.hocon.sbt

import java.io.{FileOutputStream, OutputStream, PrintStream, PrintWriter}

import com.typesafe.config._
import sbt.Keys._
import sbt.classpath.ClasspathUtilities
import sbt.{AutoPlugin, _}

import scala.collection.JavaConverters._
import scala.io.Source

object HoconPlugin extends AutoPlugin {

  object autoImport {
    lazy val hoconExtraResources = settingKey[Seq[String]]("additional reference files to consider")
    lazy val hoconPurify = inputKey[String]("Purify source HOCON configuration removing settings coniciding with defaults")
    lazy val hoconDefaults = inputKey[String]("Generate joint HOCON configuration with defaults")

    lazy val baseHoconSettings: Seq[Def.Setting[_]] = Seq(
      hoconExtraResources := Nil,
      hoconPurify := {
        val args = Def.spaceDelimited("<input> <output>").parsed
        val (i, o) = args match {
          case Seq(ii, oo) ⇒ (ii, oo)
          case _ ⇒ sys.error(s"Use: hoconPurify <input> <output>")
        }

        purify(
          loader = createLoader((fullClasspath in Compile).value),
          input = readInput(i),
          output = outputWriter(o),
          extraResources = hoconExtraResources.value
        )
      },
      hoconDefaults := {
        val args = Def.spaceDelimited("[<output>]").parsed
        if (args.size > 1) sys.error(s"Use: hoconDefaults [<output>]")
        defaults(
          loader = createLoader((fullClasspath in Compile).value),
          output = outputWriter(args.headOption getOrElse "-"),
          extraResources = hoconExtraResources.value
        )
      }
    )
  }

  import autoImport._

  override def trigger = allRequirements

  def createLoader(cp: Seq[Attributed[File]]) = ClasspathUtilities.toLoader(cp map (_.data))

  override lazy val projectSettings = baseHoconSettings
  //inConfig(Compile)(baseHoconSettings)

  def readDefaults(loader: ClassLoader, extraResources: Seq[String] = Nil) = {
    def parseResource(path: String) = ConfigFactory.parseResources(loader, path, parseOptions)
    val referenceConfig = parseResource("reference.conf")
    extraResources.foldRight(referenceConfig)(parseResource(_) withFallback _)
  }

  def purify(loader: ClassLoader, input: String, output: OutputStream, extraResources: Seq[String] = Nil) = {
    val inputConfig = ConfigFactory.parseString(input, parseOptions)
    val defaults = readDefaults(loader, extraResources)

    val inputSet = toPairSet(inputConfig)
    val defaultsSet = toPairSet(defaults)
    val defaultsMap = defaultsSet.toMap

    var diff = inputSet.diff(defaultsSet)

    if (true /* TODO: setting for cloning comments */ ) {
      diff = diff map {
        case (key, value) ⇒
          key → defaultsMap.get(key).map { v ⇒
            val referenceComments = comments(v)
            val inputComments = comments(value)
            value.withOrigin(
              v.origin.withComments(
                (if (inputComments.isEmpty) referenceComments else inputComments).asJava
              )
            )
          }.getOrElse(value)
      }
    }

    val restored = ConfigFactory.parseMap(diff.toMap.asJava)
    dump(restored, output)
  }

  def defaults(loader: ClassLoader, output: OutputStream, extraResources: Seq[String] = Nil) = {
    val config = readDefaults(loader, extraResources)
    dump(config, output)
  }

  def dump(config: Config, output: OutputStream): String = {
    val rendered = render(config)
    val writer = new PrintWriter(output)
    writer.print(rendered)
    writer.flush()
    output match {
      case _: PrintStream ⇒
      case _ ⇒ output.close()
    }
    rendered
  }

  protected val resolveOptions = ConfigResolveOptions.defaults().setAllowUnresolved(true).setUseSystemEnvironment(false)
  protected val parseOptions = ConfigParseOptions.defaults().setAllowMissing(true)

  protected def readInput(path: String): String = {
    path match {
      case "-" ⇒ Source.fromInputStream(System.in)
      case _ ⇒ Source.fromFile(path)
    }
  }.getLines() mkString "\n"

  def outputWriter(path: String) = path match {
    case "-" ⇒ System.out
    case _ ⇒ new FileOutputStream(path)
  }


  def toPairSet(config: Config) =
    config.resolve(resolveOptions).entrySet().asScala.map(e ⇒ e.getKey → e.getValue)

  def render(config: Config) =
    config.root.render(ConfigRenderOptions.defaults.setOriginComments(false).setJson(false))

  def comments(v: ConfigValue): List[String] =
    v.origin().comments().asScala.toList

}
