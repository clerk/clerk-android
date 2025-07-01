package com.clerk.network.serialization

import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type

/**
 * Internal implementation of Java's GenericArrayType interface.
 *
 * This class provides a concrete implementation of GenericArrayType for use in
 * type reflection and serialization operations. It represents generic array types
 * such as `T[]` where T is a type parameter.
 *
 * @param componentType The component type of the array
 */
internal class GenericArrayTypeImpl private constructor(private val componentType: Type) :
  GenericArrayType {
  
  /**
   * Returns the generic component type of this array type.
   *
   * @return The component type of the array
   */
  override fun getGenericComponentType() = componentType

  /**
   * Compares this GenericArrayType with another object for equality.
   *
   * Two GenericArrayType instances are equal if they have the same component type.
   *
   * @param other The object to compare with
   * @return true if the objects are equal, false otherwise
   */
  @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  override fun equals(other: Any?) =
    other is GenericArrayType && Types.equals(this, other as GenericArrayType?)

  /**
   * Returns the hash code for this GenericArrayType.
   *
   * @return Hash code based on the component type
   */
  override fun hashCode() = componentType.hashCode()

  /**
   * Returns a string representation of this generic array type.
   *
   * @return String representation in the format "ComponentType[]"
   */
  override fun toString() = componentType.typeToString() + "[]"

  companion object {
    /**
     * Factory method to create a GenericArrayTypeImpl instance.
     *
     * This method creates a new GenericArrayTypeImpl with the specified component type,
     * canonicalizing the component type before use.
     *
     * @param componentType The component type of the array
     * @return A new GenericArrayTypeImpl instance
     */
    @JvmName("create")
    @JvmStatic
    operator fun invoke(componentType: Type): GenericArrayTypeImpl {
      return GenericArrayTypeImpl(componentType.canonicalize())
    }
  }
}
