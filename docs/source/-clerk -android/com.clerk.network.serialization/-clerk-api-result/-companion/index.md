---
title: Companion
---
//[Clerk Android](../../../../index.html)/[com.clerk.network.serialization](../../index.html)/[ClerkApiResult](../index.html)/[Companion](index.html)



# Companion



[androidJvm]\
object [Companion](index.html)



## Functions


| Name | Summary |
|---|---|
| [apiFailure](api-failure.html) | [androidJvm]<br>fun &lt;[E](api-failure.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [apiFailure](api-failure.html)(error: [E](api-failure.html)? = null): [ClerkApiResult.Failure](../-failure/index.html)&lt;[E](api-failure.html)&gt;<br>Returns a new [Failure](../-failure/index.html) with API error details. |
| [httpFailure](http-failure.html) | [androidJvm]<br>fun &lt;[E](http-failure.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [httpFailure](http-failure.html)(code: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), error: [E](http-failure.html)? = null): [ClerkApiResult.Failure](../-failure/index.html)&lt;[E](http-failure.html)&gt;<br>Returns a new [Failure](../-failure/index.html) with HTTP error details. |
| [success](success.html) | [androidJvm]<br>fun &lt;[T](success.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [success](success.html)(value: [T](success.html)): [ClerkApiResult.Success](../-success/index.html)&lt;[T](success.html)&gt;<br>Returns a new [Success](../-success/index.html) with given [value](success.html). |
| [unknownFailure](unknown-failure.html) | [androidJvm]<br>fun [unknownFailure](unknown-failure.html)(throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)): [ClerkApiResult.Failure](../-failure/index.html)&lt;[Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-nothing/index.html)&gt;<br>Returns a new [Failure](../-failure/index.html) with unknown error details. |

