package com.clerk.api.network.serialization

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.reflect.KClass

@Retention(RUNTIME)
internal annotation class ResultType(
  val rawType: KClass<*>,
  val typeArgs: Array<ResultType> = [],
  val ownerType: KClass<*> = Nothing::class,
  // If it's an array, the rawType is used as the component type
  val isArray: Boolean,
)
