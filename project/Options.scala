package com.github.aalleexxeeii.hocon.sbt

import com.github.aalleexxeeii.hocon.sbt.Options._
import com.github.aalleexxeeii.hocon.sbt.Utils._
import scopt.{OptionParser, Read}

import scala.reflect.runtime.universe._

case class Options(
  commentMode: CommentMode = CommentMode.Override
)

object Options {

  val parser = new OptionParser[Options]("sbt-hocon") {
    //opt[CommentMode]("comment-mode").action((x, o) ⇒ o.copy(commentMode = x))
  }

  sealed trait CommentMode extends Enum

  object CommentMode extends EnumCompanion[CommentMode] {

    case object Off extends CommentMode

    case object Override extends CommentMode

    case object Merge extends CommentMode

  }

  trait Enum {
    val text = getClass.getName match {
      case Enum.SimplePattern(simple) ⇒ simple.toLowerCase
      case _ ⇒ getClass.getSimpleName.toLowerCase.replace("$", "")
    }

  }

  object Enum {
    protected val SimplePattern = """.*?([^.$]+)\$$""".r
  }

  abstract class EnumCompanion[E <: Enum: WeakTypeTag] {
    val values: List[E] = sealedTraitEnumObjects[E]
    val valueOf: Map[String, E] = values.map(e ⇒ e.text → e).toMap withDefault { x ⇒
      throw new IllegalArgumentException(s"Unknown comment mode '$x'")
    }
    implicit lazy val read: Read[E] = Read.reads(valueOf)
  }

}
