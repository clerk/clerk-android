package com.clerk.api.network.serialization

import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/** Internal constant representing an empty array of Type objects. */
@JvmField internal val EMPTY_TYPE_ARRAY: Array<Type> = arrayOf()

/**
 * Internal implementation of Java's WildcardType interface.
 *
 * This class provides a concrete implementation of WildcardType for use in type reflection and
 * serialization operations. It represents wildcard types such as `? extends Number` or `? super
 * String`.
 *
 * @param upperBound The upper bound of the wildcard (the type after "extends")
 * @param lowerBound The lower bound of the wildcard (the type after "super"), or null if none
 */
internal class WildcardTypeImpl
private constructor(private val upperBound: Type, private val lowerBound: Type?) : WildcardType {

  /**
   * Returns the upper bounds of this wildcard type.
   *
   * @return An array containing the single upper bound
   */
  override fun getUpperBounds() = arrayOf(upperBound)

  /**
   * Returns the lower bounds of this wildcard type.
   *
   * @return An array containing the lower bound, or an empty array if there is no lower bound
   */
  override fun getLowerBounds() = lowerBound?.let { arrayOf(it) } ?: EMPTY_TYPE_ARRAY

  /**
   * Compares this WildcardType with another object for equality.
   *
   * @param other The object to compare with
   * @return true if the objects are equal, false otherwise
   */
  @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
  override fun equals(other: Any?) =
    other is WildcardType && Types.equals(this, other as WildcardType?)

  /**
   * Returns the hash code for this WildcardType.
   *
   * @return Hash code based on the upper and lower bounds
   */
  override fun hashCode(): Int {
    // This equals Arrays.hashCode(getLowerBounds()) ^ Arrays.hashCode(getUpperBounds()).
    return (if (lowerBound != null) 31 + lowerBound.hashCode() else 1) xor
      31 + upperBound.hashCode()
  }

  /**
   * Returns a string representation of this wildcard type.
   *
   * @return String representation in wildcard format (e.g., "? extends Type", "? super Type", "?")
   */
  override fun toString(): String {
    return when {
      lowerBound != null -> "? super ${lowerBound.typeToString()}"
      upperBound === Any::class.java -> "?"
      else -> "? extends ${upperBound.typeToString()}"
    }
  }

  companion object {
    /**
     * Factory method to create a WildcardTypeImpl instance.
     *
     * This method creates a new WildcardTypeImpl with validation of bounds and canonicalization of
     * the bound types.
     *
     * @param upperBounds Array of upper bounds (must contain exactly one element)
     * @param lowerBounds Array of lower bounds (must contain at most one element)
     * @return A new WildcardTypeImpl instance
     * @throws IllegalArgumentException if bounds requirements are not met
     */
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
