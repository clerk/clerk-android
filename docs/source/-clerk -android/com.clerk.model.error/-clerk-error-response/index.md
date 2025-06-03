---
title: ClerkErrorResponse
---
//[Clerk Android](../../../index.html)/[com.clerk.model.error](../index.html)/[ClerkErrorResponse](index.html)



# ClerkErrorResponse



[androidJvm]\
@Serializable



data class [ClerkErrorResponse](index.html)(val errors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Error](../-error/index.html)&gt;, val meta: [Meta](../-meta/index.html)? = null, val clerkTraceId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))

Represents an error response from the Clerk API.



## Constructors


| | |
|---|---|
| [ClerkErrorResponse](-clerk-error-response.html) | [androidJvm]<br>constructor(errors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Error](../-error/index.html)&gt;, meta: [Meta](../-meta/index.html)? = null, clerkTraceId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [clerkTraceId](clerk-trace-id.html) | [androidJvm]<br>@SerialName(value = &quot;clerk_trace_id&quot;)<br>val [clerkTraceId](clerk-trace-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>A unique identifier for tracing the specific request, useful for debugging. |
| [errors](errors.html) | [androidJvm]<br>val [errors](errors.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Error](../-error/index.html)&gt;<br>An array of `ClerkAPIError` objects, each describing an individual error. |
| [meta](meta.html) | [androidJvm]<br>val [meta](meta.html): [Meta](../-meta/index.html)? = null<br>An object containing additional information about the error response. |

