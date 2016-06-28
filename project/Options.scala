package com.github.aalleexxeeii.hocon.sbt.opt

import com.github.aalleexxeeii.hocon.sbt.opt.CommonOptions.CommentMode
import com.github.aalleexxeeii.hocon.sbt.utils.{Enum, EnumCompanion}
import scopt.OptionParser

case class CommonOptions(
  commentMode: CommentMode = CommentMode.Override
)

object CommonOptions {

  val parser = new OptionParser[CommonOptions]("sbt-hocon") {
    //opt[CommentMode]("comment-mode").action((x, o) ⇒ o.copy(commentMode = x))
  }

  sealed trait CommentMode extends Enum

  object CommentMode extends EnumCompanion[CommentMode] {

    case object Off extends CommentMode

    case object Override extends CommentMode

    case object Merge extends CommentMode

  }

}
