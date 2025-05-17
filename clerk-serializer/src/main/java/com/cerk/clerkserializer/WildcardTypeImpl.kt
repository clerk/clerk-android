package com.cerk.clerkserializer

import java.lang.reflect.Type
import java.lang.reflect.WildcardType

@JvmField internal val EMPTY_TYPE_ARRAY: Array<Type> = arrayOf()

internal class WildcardTypeImpl
private constructor(private val upperBound: Type, private val lowerBound: Type?) : WildcardType {

  override fun getUpperBounds() = arrayOf(upperBound)

  override fun getLowerBounds() = lowerBound?.let { arrayOf(it) } ?: EMPTY_TYPE_ARRAY

  @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  override fun equals(other: Any?) =
    other is WildcardType && Types.equals(this, other as WildcardType?)

  override fun hashCode(): Int {
    // This equals Arrays.hashCode(getLowerBounds()) ^ Arrays.hashCode(getUpperBounds()).
    return (if (lowerBound != null) 31 + lowerBound.hashCode() else 1) xor
      31 + upperBound.hashCode()
  }

  override fun toString(): String {
    return when {
      lowerBound != null -> "? super ${lowerBound.typeToString()}"
      upperBound === Any::class.java -> "?"
      else -> "? extends ${upperBound.typeToString()}"
    }
  }

  companion object {
    @JvmStatic
    @JvmName("create")
    operator fun invoke(upperBounds: Array<Type>, lowerBounds: Array<Type>): WildcardTypeImpl {
      require(lowerBounds.size <= 1)
      require(upperBounds.size == 1)
      return if (lowerBounds.size == 1) {
        lowerBounds[0].checkNotPrimitive()
        require(!(upperBounds[0] !== Any::class.java))
        WildcardTypeImpl(lowerBound = lowerBounds[0].canonicalize(), upperBound = Any::class.java)
      } else {
        upperBounds[0].checkNotPrimitive()
        WildcardTypeImpl(lowerBound = null, upperBound = upperBounds[0].canonicalize())
      }
    }
  }
}
