package com.clerk.network.serialization

import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type

internal class GenericArrayTypeImpl private constructor(private val componentType: Type) :
  GenericArrayType {
  override fun getGenericComponentType() = componentType

  @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  override fun equals(other: Any?) =
    other is GenericArrayType && Types.equals(this, other as GenericArrayType?)

  override fun hashCode() = componentType.hashCode()

  override fun toString() = componentType.typeToString() + "[]"

  companion object {
    @JvmName("create")
    @JvmStatic
    operator fun invoke(componentType: Type): GenericArrayTypeImpl {
      return GenericArrayTypeImpl(componentType.canonicalize())
    }
  }
}
