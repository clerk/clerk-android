package com.clerk.network.serialization

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

internal class ParameterizedTypeImpl
private constructor(
  private val ownerType: Type?,
  private val rawType: Type,
  @JvmField val typeArguments: Array<Type>,
) : ParameterizedType {
  override fun getActualTypeArguments() = typeArguments.clone()

  override fun getRawType() = rawType

  override fun getOwnerType() = ownerType

  @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  override fun equals(other: Any?) =
    other is ParameterizedType && Types.equals(this, other as ParameterizedType?)

  override fun hashCode(): Int {
    return typeArguments.contentHashCode() xor rawType.hashCode() xor ownerType.hashCodeOrZero
  }

  @Suppress("MagicNumber")
  override fun toString(): String {
    val result = StringBuilder(30 * (typeArguments.size + 1))
    result.append(rawType.typeToString())
    if (typeArguments.isEmpty()) {
      return result.toString()
    }
    result.append("<").append(typeArguments[0].typeToString())
    for (i in 1 until typeArguments.size) {
      result.append(", ").append(typeArguments[i].typeToString())
    }
    return result.append(">").toString()
  }

  companion object {
    @JvmName("create")
    @JvmStatic
    operator fun invoke(
      ownerType: Type?,
      rawType: Type,
      vararg typeArguments: Type,
    ): ParameterizedTypeImpl {
      // Require an owner type if the raw type needs it.
      if (rawType is Class<*>) {
        val enclosingClass = rawType.enclosingClass
        if (ownerType != null) {
          require(!(enclosingClass == null || ownerType.rawType != enclosingClass)) {
            "unexpected owner type for $rawType: $ownerType"
          }
        } else {
          require(enclosingClass == null) { "unexpected owner type for $rawType: null" }
        }
      }
      @Suppress("UNCHECKED_CAST") val finalTypeArgs = typeArguments.clone() as Array<Type>
      for (t in finalTypeArgs.indices) {
        finalTypeArgs[t].checkNotPrimitive()
        finalTypeArgs[t] = finalTypeArgs[t].canonicalize()
      }
      return ParameterizedTypeImpl(ownerType?.canonicalize(), rawType.canonicalize(), finalTypeArgs)
    }
  }
}

@Suppress(
  "SpreadOperator",
  "ReturnCount",
  "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
)
fun Type.canonicalize(): Type {
  return when (this) {
    is Class<*> -> {
      if (isArray) GenericArrayTypeImpl(this@canonicalize.componentType.canonicalize()) else this
    }
    is ParameterizedType -> {
      if (this is ParameterizedTypeImpl) return this
      ParameterizedTypeImpl(ownerType, rawType, *actualTypeArguments)
    }
    is GenericArrayType -> {
      if (this is GenericArrayTypeImpl) return this
      GenericArrayTypeImpl(genericComponentType)
    }
    is WildcardType -> {
      if (this is WildcardTypeImpl) return this
      WildcardTypeImpl(upperBounds, lowerBounds)
    }
    else -> this // This type is unsupported!
  }
}

internal fun Type.checkNotPrimitive() {
  require(!(this is Class<*> && isPrimitive)) { "Unexpected primitive $this. Use the boxed type." }
}

internal fun Type.typeToString(): String {
  return if (this is Class<*>) name else toString()
}

internal val Any?.hashCodeOrZero: Int
  get() {
    return this?.hashCode() ?: 0
  }
