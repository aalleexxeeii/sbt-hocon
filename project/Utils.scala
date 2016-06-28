package com.github.aalleexxeeii.hocon.sbt.utils

import com.github.aalleexxeeii.hocon.sbt.utils.Utils._
import scopt.Read

import scala.reflect.runtime.universe._

object Utils {
  protected val mirror = runtimeMirror(this.getClass.getClassLoader)

  def sealedTraitEnumObjects[E: WeakTypeTag]: List[E with Product with Serializable] = {
    val tpe = weakTypeOf[E]
    val symbol = tpe.typeSymbol.asClass
    require(symbol.isSealed, s"Type $tpe must be sealed")

    for {
      subclass ← symbol.knownDirectSubclasses.toList
      if subclass.asClass.isCaseClass
      moduleSymbol = mirror.staticModule(subclass.fullName)
    } yield mirror.reflectModule(moduleSymbol).instance.asInstanceOf[E with Product with Serializable]
  }

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

abstract class EnumCompanion[E <: Enum : WeakTypeTag] {
  val values: List[E] = sealedTraitEnumObjects[E]
  val valueOf: Map[String, E] = values.map(e ⇒ e.text → e).toMap withDefault { x ⇒
    throw new IllegalArgumentException(s"Unknown comment mode '$x'")
  }
  implicit lazy val read: Read[E] = Read.reads(valueOf)
}

