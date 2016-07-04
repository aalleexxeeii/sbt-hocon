package com.github.aalleexxeeii.hocon.sbt

import java.io.{FileOutputStream, OutputStream, PrintStream, PrintWriter}

import com.github.aalleexxeeii.hocon.sbt.opt.Common.{CommentMode, _}
import com.github.aalleexxeeii.hocon.sbt.opt.{Common, Defaults, Purify}
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
        val args = Def.spaceDelimited(Purify.parser.usage).parsed
        Purify.parser.parse(args, Purify()) map { opt ⇒
          purify(
            loader = createLoader((fullClasspath in Compile).value),
            input = readInput(opt.input),
            output = outputWriter(opt.output),
            extraResources = hoconExtraResources.value,
            options = opt
          )
        } getOrElse sys.error("Wrong arguments")
      },
      hoconDefaults := {
        val args = Def.spaceDelimited(Defaults.parser.usage).parsed
        Defaults.parser.parse(args, Defaults()) map { opt ⇒
          defaults(
            loader = createLoader((fullClasspath in Compile).value),
            output = outputWriter(opt.output),
            extraResources = hoconExtraResources.value,
            options = opt
          )
        } getOrElse sys.error("Wrong arguments")
      }
    )
  }

  import autoImport._

  override def trigger = allRequirements

  def createLoader(cp: Seq[Attributed[File]]) = ClasspathUtilities.toLoader(cp map (_.data))

  override lazy val projectSettings = baseHoconSettings
  //inConfig(Compile)(baseHoconSettings)

  def readDefaults(loader: ClassLoader, extraResources: Seq[String] = Nil, options: Common) = {
    def parseResource(path: String) = escapeUnresolved(
      ConfigFactory.parseResources(loader, path, parseOptions),
      options
    )
    val referenceConfig = parseResource("reference.conf")
    extraResources.foldRight(referenceConfig)(parseResource(_) withFallback _)
  }

  def purify(loader: ClassLoader, input: String, output: OutputStream, extraResources: Seq[String] = Nil, options: Purify) = {
    val inputConfig = escapeUnresolved(ConfigFactory.parseString(input, parseOptions), options.common)
    val defaults = readDefaults(loader, extraResources, options.common)

    val inputSet = toPairSet(inputConfig)
    val defaultsSet = toPairSet(defaults)
    val defaultsMap = defaultsSet.toMap

    var diff = inputSet.diff(defaultsSet)

    if (options.common.commentMode != CommentMode.Off) {
      diff = diff map {
        case (key, value) ⇒
          key → {
            defaultsMap.get(key) map { v ⇒
              val referenceComments = comments(v)
              val inputComments = comments(value)
              value.withOrigin(
                v.origin.withComments(
                  (options.common.commentMode match {
                    case CommentMode.Override ⇒ if (inputComments.isEmpty) referenceComments else inputComments
                    case CommentMode.Merge ⇒ referenceComments ::: inputComments
                    case unsupported ⇒ sys.error(s"Unsupported comment mode: $unsupported")
                  }).asJava
                )
              )
            } getOrElse value
          }
      }
    }

    val restored = ConfigFactory.parseMap(diff.toMap.asJava)
    val raw = render(restored, options.common)
    val unescaped = unescape(raw)
    dump(unescaped, output)
  }

  def defaults(loader: ClassLoader, output: OutputStream, extraResources: Seq[String] = Nil, options: Defaults) = {
    val config = readDefaults(loader, extraResources, options.common)
    dump(unescape(render(config, options.common)), output)
  }

  private val EscapePrefix = "\ufff0"
  private val EscapeSuffix = "\ufff1"
  private val EscapedPattern = s"""(?m)\"$EscapePrefix(.+)$EscapeSuffix\"""".r

  def escapeUnresolved(config: Config, options: Common): Config = {
    val unmergeables = toPairSet(config) collect {
      case (path, v@UnmergeableBridge()) ⇒
        val raw = v.render(renderOptions.setOriginComments(options.originComments))
        path → ConfigValueFactory.fromAnyRef(s"$EscapePrefix$raw$EscapeSuffix").withOrigin(v.origin())
    }
    unmergeables.foldLeft(config) { case (c, (path, value)) ⇒ c.withValue(path, value) }
  }

  protected def unescape(text: String) =
    EscapedPattern.replaceAllIn(text, { m ⇒
      val quoted = m.group(1)
      val unquoted = ConfigFactory.parseString(s"""x="$quoted"""").getString("x")
      unquoted.replace("$", "\\$")
    })

  protected def dump(config: Config, output: OutputStream): String = {
    dump(render(config), output)
  }

  protected def dump(text: String, output: OutputStream): String = {
    val writer = new PrintWriter(output)
    writer.print(text)
    writer.flush()
    output match {
      case _: PrintStream ⇒
      case _ ⇒ output.close()
    }
    text
  }

  protected val resolveOptions = ConfigResolveOptions.defaults().setAllowUnresolved(true).setUseSystemEnvironment(false)
  protected val parseOptions = ConfigParseOptions.defaults().setAllowMissing(true)
  protected val renderOptions = ConfigRenderOptions.defaults().setOriginComments(false).setJson(false)

  protected def readInput(path: String): String = {
    path match {
      case StdStreamSymbol ⇒ Source.fromInputStream(System.in)
      case _ ⇒ Source.fromFile(path)
    }
  }.getLines() mkString "\n"

  def outputWriter(path: String) = path match {
    case StdStreamSymbol ⇒ System.out
    case _ ⇒ new FileOutputStream(path)
  }


  def toPairSet(config: Config) =
    config.resolve(resolveOptions).entrySet().asScala.map(e ⇒ e.getKey → e.getValue)

  def render(config: Config, options: Common = Common()) =
    applyPathRestrictions(config, options).root.render(ConfigRenderOptions.defaults
      .setComments(options.commentMode != CommentMode.Off)
      .setOriginComments(options.originComments)
      .setJson(false)
    )

  def applyPathRestrictions(config: Config, options: Common) =
    options.exclusions.foldLeft {
      if (options.inclusions.isEmpty) config
      else options.inclusions map config.withOnlyPath reduce (_ withFallback _)
    }(_ withoutPath _)

  def comments(v: ConfigValue): List[String] =
    v.origin().comments().asScala.toList

  private object UnmergeableBridge {
    // reflection due to package access level in impl.* classes and cross-classloader issues
    private val classUnmergeable = Class.forName("com.typesafe.config.impl.Unmergeable", true, getClass.getClassLoader)

    def unapply(v: ConfigValue): Boolean =
      classUnmergeable isInstance v
  }

}
