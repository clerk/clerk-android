---
title: ClerkApiResult
---
//[Clerk Android](../../../index.html)/[com.clerk.network.serialization](../index.html)/[ClerkApiResult](index.html)



# ClerkApiResult

sealed interface [ClerkApiResult](index.html)&lt;out [T](index.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), out [E](index.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;

ClerkApiService is an internal interface that defines the API endpoints for the Clerk authentication service.



This interface is not meant to be used directly by SDK consumers. Instead, all API operations should be accessed through the ClerkApi.instance singleton, which provides a more user-friendly API surface.



The interface handles user authentication, session management, sign-up and sign-in flows, and environment information using Retrofit annotations to define the HTTP methods and endpoints.



All endpoints return a [ClerkApiResult](index.html), which is a sealed type that represents either a successful response with the expected data ([ClerkApiResult.Success](-success/index.html)) or a failure ([ClerkApiResult.Failure](-failure/index.html)). This approach allows for non-exceptional, type-safe handling of API responses.



# Usage Example

```kotlin
// Example: Creating a sign-in
scope.launch {
    val email = "user@example.com"
    when (val result = ClerkApi.instance.signIn(email)) {
        is ClerkApiResult.Success -> {
            val signIn = result.value.response
            // Proceed with the sign-in flow based on available factors
            val firstFactor = signIn.supportedFirstFactors.firstOrNull()
            // Handle first factor preparation
        }
        is ClerkApiResult.Failure -> {
            // Handle sign-in failure
        }
    }
}
```


#### Inheritors


| |
|---|
| [Success](-success/index.html) |
| [Failure](-failure/index.html) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [androidJvm]<br>object [Companion](-companion/index.html) |
| [Failure](-failure/index.html) | [androidJvm]<br>class [Failure](-failure/index.html)&lt;out [E](-failure/index.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;(val error: [E](-failure/index.html)?, val throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null, val code: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null, val errorType: [ClerkApiResult.Failure.ErrorType](-failure/-error-type/index.html) = ErrorType.UNKNOWN, tags: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; = emptyMap()) : [ClerkApiResult](index.html)&lt;[Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-nothing/index.html), [E](-failure/index.html)&gt; <br>A unified failure type that contains all necessary error information. |
| [Success](-success/index.html) | [androidJvm]<br>class [Success](-success/index.html)&lt;out [T](-success/index.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;(val value: [T](-success/index.html), tags: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;) : [ClerkApiResult](index.html)&lt;[T](-success/index.html), [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-nothing/index.html)&gt; <br>A successful result with the data available in [value](-success/value.html). |


## Functions


| Name | Summary |
|---|---|
| [flatMap](../flat-map.html) | [androidJvm]<br>inline fun &lt;[T](../flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [R](../flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](../flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](index.html)&lt;[T](../flat-map.html), [E](../flat-map.html)&gt;.[flatMap](../flat-map.html)(transform: (value: [T](../flat-map.html)) -&gt; [ClerkApiResult](index.html)&lt;[R](../flat-map.html), [E](../flat-map.html)&gt;): [ClerkApiResult](index.html)&lt;[R](../flat-map.html), [E](../flat-map.html)&gt;<br>Returns a new [ClerkApiResult](index.html) by applying [transform](../flat-map.html) to the value of a [ClerkApiResult.Success](-success/index.html), or returns the original [ClerkApiResult.Failure](-failure/index.html) if this is a failure. |
| [fold](../fold.html) | [androidJvm]<br>inline fun &lt;[T](../fold.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](../fold.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [C](../fold.html)&gt; [ClerkApiResult](index.html)&lt;[T](../fold.html), [E](../fold.html)&gt;.[fold](../fold.html)(onSuccess: (value: [T](../fold.html)) -&gt; [C](../fold.html), onFailure: (failure: [ClerkApiResult.Failure](-failure/index.html)&lt;[E](../fold.html)&gt;) -&gt; [C](../fold.html)): [C](../fold.html)<br>Transforms an [ClerkApiResult](index.html) into a [C](../fold.html) value. |
| [onFailure](../on-failure.html) | [androidJvm]<br>inline fun &lt;[T](../on-failure.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](../on-failure.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](index.html)&lt;[T](../on-failure.html), [E](../on-failure.html)&gt;.[onFailure](../on-failure.html)(action: (failure: [ClerkApiResult.Failure](-failure/index.html)&lt;[E](../on-failure.html)&gt;) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)): [ClerkApiResult](index.html)&lt;[T](../on-failure.html), [E](../on-failure.html)&gt;<br>Performs the given [action](../on-failure.html) on the encapsulated [ClerkApiResult.Failure](-failure/index.html) if this instance represents [failure](-failure/index.html). Returns the original `ClerkApiResult` unchanged. |
| [onFailureType](../on-failure-type.html) | [androidJvm]<br>inline fun &lt;[T](../on-failure-type.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](../on-failure-type.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](index.html)&lt;[T](../on-failure-type.html), [E](../on-failure-type.html)&gt;.[onFailureType](../on-failure-type.html)(errorType: [ClerkApiResult.Failure.ErrorType](-failure/-error-type/index.html), action: (failure: [ClerkApiResult.Failure](-failure/index.html)&lt;[E](../on-failure-type.html)&gt;) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)): [ClerkApiResult](index.html)&lt;[T](../on-failure-type.html), [E](../on-failure-type.html)&gt;<br>Performs the given [action](../on-failure-type.html) on the encapsulated failure if this instance represents a failure with the specified error type. Returns the original `ClerkApiResult` unchanged. |
| [onSuccess](../on-success.html) | [androidJvm]<br>inline fun &lt;[T](../on-success.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](../on-success.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](index.html)&lt;[T](../on-success.html), [E](../on-success.html)&gt;.[onSuccess](../on-success.html)(action: (value: [T](../on-success.html)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)): [ClerkApiResult](index.html)&lt;[T](../on-success.html), [E](../on-success.html)&gt;<br>Performs the given [action](../on-success.html) on the encapsulated value if this instance represents [success](-success/index.html). Returns the original `ClerkApiResult` unchanged. |
| [successOrElse](../success-or-else.html) | [androidJvm]<br>inline fun &lt;[T](../success-or-else.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](../success-or-else.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](index.html)&lt;[T](../success-or-else.html), [E](../success-or-else.html)&gt;.[successOrElse](../success-or-else.html)(defaultValue: (failure: [ClerkApiResult.Failure](-failure/index.html)&lt;[E](../success-or-else.html)&gt;) -&gt; [T](../success-or-else.html)): [T](../success-or-else.html)<br>If [ClerkApiResult.Success](-success/index.html), returns the underlying [T](../success-or-else.html) value. Otherwise, returns the result of the [defaultValue](../success-or-else.html) function. |
| [successOrNothing](../success-or-nothing.html) | [androidJvm]<br>inline fun &lt;[T](../success-or-nothing.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](../success-or-nothing.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](index.html)&lt;[T](../success-or-nothing.html), [E](../success-or-nothing.html)&gt;.[successOrNothing](../success-or-nothing.html)(body: (failure: [ClerkApiResult.Failure](-failure/index.html)&lt;[E](../success-or-nothing.html)&gt;) -&gt; [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-nothing/index.html)): [T](../success-or-nothing.html)<br>If [ClerkApiResult.Success](-success/index.html), returns the underlying [T](../success-or-nothing.html) value. Otherwise, calls [body](../success-or-nothing.html) with the failure, which can either throw an exception or return early (since this function is inline). |
| [successOrNull](../success-or-null.html) | [androidJvm]<br>fun &lt;[T](../success-or-null.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](../success-or-null.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](index.html)&lt;[T](../success-or-null.html), [E](../success-or-null.html)&gt;.[successOrNull](../success-or-null.html)(): [T](../success-or-null.html)?<br>If [ClerkApiResult.Success](-success/index.html), returns the underlying [T](../success-or-null.html) value. Otherwise, returns null. |
| [suspendingFlatMap](../suspending-flat-map.html) | [androidJvm]<br>inline suspend fun &lt;[T](../suspending-flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [R](../suspending-flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](../suspending-flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](index.html)&lt;[T](../suspending-flat-map.html), [E](../suspending-flat-map.html)&gt;.[suspendingFlatMap](../suspending-flat-map.html)(transform: suspend (value: [T](../suspending-flat-map.html)) -&gt; [ClerkApiResult](index.html)&lt;[R](../suspending-flat-map.html), [E](../suspending-flat-map.html)&gt;): [ClerkApiResult](index.html)&lt;[R](../suspending-flat-map.html), [E](../suspending-flat-map.html)&gt;<br>Returns a new [ClerkApiResult](index.html) by applying [transform](../suspending-flat-map.html) to the value of a [ClerkApiResult.Success](-success/index.html), or returns the original [ClerkApiResult.Failure](-failure/index.html) if this is a failure. |
| [suspendingFold](../suspending-fold.html) | [androidJvm]<br>inline suspend fun &lt;[T](../suspending-fold.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](../suspending-fold.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [C](../suspending-fold.html)&gt; [ClerkApiResult](index.html)&lt;[T](../suspending-fold.html), [E](../suspending-fold.html)&gt;.[suspendingFold](../suspending-fold.html)(noinline onSuccess: suspend (value: [T](../suspending-fold.html)) -&gt; [C](../suspending-fold.html), noinline onFailure: (failure: [ClerkApiResult.Failure](-failure/index.html)&lt;[E](../suspending-fold.html)&gt;) -&gt; [C](../suspending-fold.html)): [C](../suspending-fold.html)<br>Transforms an [ClerkApiResult](index.html) into a [C](../suspending-fold.html) value. |

