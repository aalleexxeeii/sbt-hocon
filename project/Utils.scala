package com.github.aalleexxeeii.hocon.sbt

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
