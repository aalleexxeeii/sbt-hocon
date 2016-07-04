package com.github.aalleexxeeii.hocon.sbt.opt

import com.github.aalleexxeeii.hocon.sbt.opt.Common.{CommentMode, StdStreamSymbol}
import com.github.aalleexxeeii.hocon.sbt.utils.{Enum, EnumCompanion}
import scopt.OptionParser

case class Common(
  commentMode: CommentMode = CommentMode.Override,
  originComments: Boolean = false,
  inclusions: List[String] = Nil,
  exclusions: List[String] = Nil
)

object Common {
  final val StdStreamSymbol = "@"

  trait With[T] {
    def common: Common

    def combine(common: Common): T
  }

  trait Parser[T <: Common.With[T]] extends OptionParser[T] {
    opt[CommentMode]('c', "comments")
      .optional()
      .valueName(CommentMode.valueOf.keys.mkString("|"))
      .text("Mode for comments: 'off' - no comments; 'override' - use top-level comment (default); 'merge' - merge all comments together")
      .action((x, o) ⇒ o.combine(o.common.copy(commentMode = x)))

    opt[Unit]("origin-comments")
      .optional()
      .text("Include origin in comments")
      .action((_, o) ⇒ o.combine(o.common.copy(originComments = true)))

    opt[Seq[String]]('i', "include")
      .valueName("<path1>[,<path2>...]")
      .text("Include just the given paths")
      .action((x, o) ⇒ o.combine(o.common.copy(inclusions = o.common.inclusions ++ x)))

    opt[Seq[String]]('x', "exclude")
      .valueName("<path1>[,<path2>...]")
      .text("Exclude the given paths")
      .action((x, o) ⇒ o.combine(o.common.copy(exclusions = o.common.exclusions ++ x)))

    // help("help") -- terminates JVM
    opt[Unit]('h', "help")
      .text("Show this help")
      .action { (_, c) ⇒ showUsage(); c }
      .validate(_ ⇒ Left("Help requested"))
  }

  sealed trait CommentMode extends Enum

  object CommentMode extends EnumCompanion[CommentMode] {

    case object Off extends CommentMode

    case object Override extends CommentMode

    case object Merge extends CommentMode

  }

}

case class Defaults(
  common: Common = Common(),
  output: String = StdStreamSymbol
) extends Common.With[Defaults] {
  override def combine(common: Common): Defaults = copy(common = common)
}

object Defaults {
  val parser = new OptionParser[Defaults]("hoconDefaults") with Common.Parser[Defaults] {
    head("Generate full reference configuration discovered from project dependencies")
    arg[String]("<output>")
      .optional()
      .text(s"Output file. Specify '$StdStreamSymbol' for stdout")
      .action((x, o) ⇒ o.copy(output = x))
  }
}

case class Purify(
  common: Common = Common(),
  input: String = StdStreamSymbol,
  output: String = StdStreamSymbol
) extends Common.With[Purify] {
  override def combine(common: Common): Purify = copy(common = common)
}

object Purify {
  val parser = new OptionParser[Purify]("hoconPurify") with Common.Parser[Purify] {
    head("Purify custom HOCON files by removing settings coniciding with defaults, supplying with comments taken from reference configuration, formatting with proper structure and indentation")
    arg[String]("<input>")
      .required()
      .text(s"Input file. Specify '$StdStreamSymbol' for stdin")
      .action((x, o) ⇒ o.copy(input = x))
    arg[String]("<output>")
      .optional()
      .text(s"Output file. Specify '$StdStreamSymbol' for stdout")
      .action((x, o) ⇒ o.copy(output = x))
  }
}
