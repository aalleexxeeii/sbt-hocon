package aalleexxeeii.hocon.sbt

import java.io.{FileOutputStream, PrintWriter}

import com.typesafe.config._
import sbt.Keys._
import sbt.classpath.ClasspathUtilities
import sbt.std.TaskStreams
import sbt.{AutoPlugin, _}

import scala.collection.JavaConverters._
import scala.io.Source

object HoconPlugin extends AutoPlugin {

  object autoImport {
    lazy val hoconExtraResources = settingKey[Seq[String]]("additional reference files to consider")
    lazy val hoconPurify = inputKey[String]("Purify source HOCON configuration removing settings coniciding with defaults")

    lazy val basePurifySettings: Seq[Def.Setting[_]] = Seq(
      hoconExtraResources := Nil,
      hoconPurify := {
        val logger = streams.value.log
        val args = Def.spaceDelimited("<input> <output>").parsed
        val (i, o) = args match {
          case Seq(i, o) ⇒ (i, o)
          case _ ⇒ sys.error(s"Use: purify <input> <output>")
        }
        val taskStreams = streams.value
        val cp: Seq[Attributed[File]] = (fullClasspath in Compile).value
        val loader = ClasspathUtilities.toLoader(cp map (_.data))
        val inputSource = i match {
          case "-" ⇒ Source.fromInputStream(System.in)
          case path ⇒ Source.fromFile(path)
        }
        val outputWriter = o match {
          case "-" ⇒
            //streams.value.text()
            new PrintWriter(System.out)
          case path ⇒ new PrintWriter(new FileOutputStream(path))
        }

        val input = inputSource.getLines() mkString "\n"
        apply(loader = loader, output = outputWriter, input = input, streams = taskStreams, extraResources = hoconExtraResources.value)
      }
    )
  }

  import autoImport._

  override val projectSettings = basePurifySettings
  //inConfig(Compile)(basePurifySettings)

  def apply(loader: ClassLoader, input: String, output: PrintWriter, extraResources: Seq[String] = Nil, streams: TaskStreams[_]) = {
    val logger = streams.log
    val inputConfig = ConfigFactory.parseString(input, parseOptions)
    def parse(path: String) =
      ConfigFactory.parseResources(loader, path, parseOptions)
    val referenceConfig = parse("reference.conf")

    val completeConfig = extraResources.foldRight(referenceConfig)(parse(_) withFallback _)

    val inputSet = toPairSet(inputConfig)
    val referenceSet = toPairSet(completeConfig)
    val referenceMap = referenceSet.toMap

    var diff = inputSet.diff(referenceSet)

    if (true /* TODO: setting for cloning comments */ ) {
      diff = diff map {
        case (key, value) ⇒
          key → referenceMap.get(key).map { v ⇒
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
    val rendered = render(restored)
    output.print(rendered)
    output.close()
    rendered
  }


  protected val resolveOptions = ConfigResolveOptions.defaults().setAllowUnresolved(true).setUseSystemEnvironment(false)
  protected val parseOptions = ConfigParseOptions.defaults().setAllowMissing(true)

  def toPairSet(config: Config) =
    config.resolve(resolveOptions).entrySet().asScala.map(e ⇒ e.getKey → e.getValue)

  def render(config: Config) =
    config.root.render(ConfigRenderOptions.defaults.setOriginComments(false).setJson(false))

  def comments(v: ConfigValue): List[String] =
    v.origin().comments().asScala.toList

}
