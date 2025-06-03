---
title: ClerkApiResultCallAdapterFactory
---
//[Clerk Android](../../../index.html)/[com.clerk.network.serialization](../index.html)/[ClerkApiResultCallAdapterFactory](index.html)



# ClerkApiResultCallAdapterFactory



[androidJvm]\
object [ClerkApiResultCallAdapterFactory](index.html) : CallAdapter.Factory

A custom CallAdapter.Factory for [ClerkApiResult](../-clerk-api-result/index.html) calls. This creates a delegating adapter for suspend function calls that return [ClerkApiResult](../-clerk-api-result/index.html). This facilitates returning all error types through a single [ClerkApiResult.Failure](../-clerk-api-result/-failure/index.html) type.



## Functions


| Name | Summary |
|---|---|
| [get](get.html) | [androidJvm]<br>open operator override fun [get](get.html)(returnType: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html), annotations: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Annotation](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-annotation/index.html)&gt;, retrofit: Retrofit): CallAdapter&lt;*, *&gt;? |

