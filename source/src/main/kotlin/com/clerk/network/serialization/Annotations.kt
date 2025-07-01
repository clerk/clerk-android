/**
 * Internal serialization utilities for the Clerk API result handling.
 *
 * This file contains utility functions for creating status codes, result types, and handling
 * Java reflection types in the context of Clerk API serialization and deserialization.
 */
@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.clerk.network.serialization

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * Creates a status code wrapper after validating the HTTP status code.
 *
 * This function validates that the provided HTTP status code represents a failure
 * condition before wrapping it in a StatusCode instance.
 *
 * @param code The HTTP status code to wrap
 * @return A StatusCode instance containing the validated code
 * @throws IllegalArgumentException if the code doesn't represent a failure
 */
internal fun createStatusCode(code: Int): StatusCode {
  ClerkResult.checkHttpFailureCode(code)
  return StatusCode(code)
}

/**
 * Creates a ResultType from a Java reflection Type.
 *
 * This function analyzes the provided Type and creates a corresponding ResultType
 * that can be used for serialization and deserialization operations. It handles
 * various type scenarios including classes, parameterized types, arrays, and wildcards.
 *
 * @param type The Java reflection Type to convert
 * @return A ResultType representing the structure of the input type
 * @throws IllegalStateException if the type is not recognized
 */
internal fun createResultType(type: Type): ResultType {
  var ownerType: Type = Nothing::class.java
  val rawType: Class<*>
  val typeArgs: Array<ResultType>
  var isArray = false
  when (type) {
    is Class<*> -> {
      typeArgs = emptyArray()
      if (type.isArray) {
        rawType = type.componentType
        isArray = true
      } else {
        rawType = type
      }
    }
    is ParameterizedType -> {
      ownerType = type.ownerType ?: Nothing::class.java
      rawType = Types.getRawType(type)
      typeArgs =
        Array(type.actualTypeArguments.size) { i -> createResultType(type.actualTypeArguments[i]) }
    }
    is WildcardType -> return createResultType(type.removeSubtypeWildcard())
    else -> error("Unrecognized type: $type")
  }

  return ResultType(
    ownerType = (ownerType.canonicalize() as Class<*>).kotlin,
    rawType = (rawType.canonicalize() as Class<*>).kotlin,
    typeArgs = typeArgs,
    isArray = isArray,
  )
}

/**
 * Extension function to remove subtype wildcards from a Type.
 *
 * This function handles wildcard types by removing subtype wildcards (? extends T)
 * and returning the upper bound type. It leaves other wildcard types unchanged.
 *
 * @return The type with subtype wildcards removed, or the original type if no wildcards
 */
@Suppress("ReturnCount")
internal fun Type.removeSubtypeWildcard(): Type {
  if (this !is WildcardType) return this
  val lowerBounds = lowerBounds
  if (lowerBounds.isNotEmpty()) return this
  val upperBounds = upperBounds
  require(upperBounds.size == 1)
  return upperBounds[0]
}
