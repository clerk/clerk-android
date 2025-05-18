package com.clerk.clerkserializer

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

public fun createStatusCode(code: Int): StatusCode {
  ClerkApiResult.checkHttpFailureCode(code)
  return StatusCode(code)
}

public fun createResultType(type: Type): ResultType {
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

@Suppress("ReturnCount")
public fun Type.removeSubtypeWildcard(): Type {
  if (this !is WildcardType) return this
  val lowerBounds = lowerBounds
  if (lowerBounds.isNotEmpty()) return this
  val upperBounds = upperBounds
  require(upperBounds.size == 1)
  return upperBounds[0]
}
