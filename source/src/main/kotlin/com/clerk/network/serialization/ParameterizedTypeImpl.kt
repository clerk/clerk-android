package com.clerk.network.serialization

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * Internal implementation of Java's ParameterizedType interface.
 *
 * This class provides a concrete implementation of ParameterizedType for use in
 * type reflection and serialization operations. It represents parameterized types
 * such as `List<String>` or `Map<String, Integer>`.
 *
 * @param ownerType The owner type, if this is a nested type
 * @param rawType The raw type (e.g., List for List<String>)
 * @param typeArguments The type arguments (e.g., [String] for List<String>)
 */
internal class ParameterizedTypeImpl
private constructor(
  private val ownerType: Type?,
  private val rawType: Type,
  @JvmField val typeArguments: Array<Type>,
) : ParameterizedType {
  
  /**
   * Returns the actual type arguments of this parameterized type.
   *
   * @return A clone of the type arguments array
   */
  override fun getActualTypeArguments() = typeArguments.clone()

  /**
   * Returns the raw type of this parameterized type.
   *
   * @return The raw type (e.g., List for List<String>)
   */
  override fun getRawType() = rawType

  /**
   * Returns the owner type of this parameterized type.
   *
   * @return The owner type, or null if this is not a nested type
   */
  override fun getOwnerType() = ownerType

  /**
   * Compares this ParameterizedType with another object for equality.
   *
   * @param other The object to compare with
   * @return true if the objects are equal, false otherwise
   */
  @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  override fun equals(other: Any?) =
    other is ParameterizedType && Types.equals(this, other as ParameterizedType?)

  /**
   * Returns the hash code for this ParameterizedType.
   *
   * @return Hash code based on type arguments, raw type, and owner type
   */
  override fun hashCode(): Int {
    return typeArguments.contentHashCode() xor rawType.hashCode() xor ownerType.hashCodeOrZero
  }

  /**
   * Returns a string representation of this parameterized type.
   *
   * @return String representation in the format "RawType<Arg1, Arg2, ...>"
   */
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
    /**
     * Factory method to create a ParameterizedTypeImpl instance.
     *
     * This method creates a new ParameterizedTypeImpl with validation of owner types
     * and canonicalization of all type arguments.
     *
     * @param ownerType The owner type, if this is a nested type
     * @param rawType The raw type
     * @param typeArguments The type arguments
     * @return A new ParameterizedTypeImpl instance
     * @throws IllegalArgumentException if owner type requirements are not met
     */
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

/**
 * Extension function to canonicalize a Type.
 *
 * This function converts various Type implementations to their canonical forms,
 * ensuring consistent representation and behavior across the type system.
 *
 * @return The canonical form of this Type
 */
@Suppress(
  "SpreadOperator",
  "ReturnCount",
  "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
)
internal fun Type.canonicalize(): Type {
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

/**
 * Extension function to check that a Type is not a primitive type.
 *
 * This function validates that the Type is not a primitive Java type, as
 * primitive types are not allowed in generic type arguments.
 *
 * @throws IllegalArgumentException if this Type is a primitive type
 */
internal fun Type.checkNotPrimitive() {
  require(!(this is Class<*> && isPrimitive)) { "Unexpected primitive $this. Use the boxed type." }
}

/**
 * Extension function to get a string representation of a Type.
 *
 * This function provides a consistent string representation for Types,
 * using the class name for Class types and toString() for others.
 *
 * @return String representation of this Type
 */
internal fun Type.typeToString(): String {
  return if (this is Class<*>) name else toString()
}

/**
 * Extension property to get the hash code of an object, or zero if null.
 *
 * This property provides a safe way to get hash codes that handles null values
 * by returning zero instead of throwing an exception.
 */
internal val Any?.hashCodeOrZero: Int
  get() {
    return this?.hashCode() ?: 0
  }
