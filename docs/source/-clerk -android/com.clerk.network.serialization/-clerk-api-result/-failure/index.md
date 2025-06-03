---
title: Failure
---
//[Clerk Android](../../../../index.html)/[com.clerk.network.serialization](../../index.html)/[ClerkApiResult](../index.html)/[Failure](index.html)



# Failure



[androidJvm]\
class [Failure](index.html)&lt;out [E](index.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;(val error: [E](index.html)?, val throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null, val code: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null, val errorType: [ClerkApiResult.Failure.ErrorType](-error-type/index.html) = ErrorType.UNKNOWN, tags: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; = emptyMap()) : [ClerkApiResult](../index.html)&lt;[Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-nothing/index.html), [E](index.html)&gt; 

A unified failure type that contains all necessary error information.



## Constructors


| | |
|---|---|
| [Failure](-failure.html) | [androidJvm]<br>constructor(error: [E](index.html)?, throwable: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null, code: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null, errorType: [ClerkApiResult.Failure.ErrorType](-error-type/index.html) = ErrorType.UNKNOWN, tags: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; = emptyMap()) |


## Types


| Name | Summary |
|---|---|
| [ErrorType](-error-type/index.html) | [androidJvm]<br>enum [ErrorType](-error-type/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[ClerkApiResult.Failure.ErrorType](-error-type/index.html)&gt; |


## Properties


| Name | Summary |
|---|---|
| [code](code.html) | [androidJvm]<br>val [code](code.html): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null |
| [error](error.html) | [androidJvm]<br>val [error](error.html): [E](index.html)? |
| [errorType](error-type.html) | [androidJvm]<br>val [errorType](error-type.html): [ClerkApiResult.Failure.ErrorType](-error-type/index.html) |
| [tags](tags.html) | [androidJvm]<br>val [tags](tags.html): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; |
| [throwable](throwable.html) | [androidJvm]<br>val [throwable](throwable.html): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)? = null |


## Functions


| Name | Summary |
|---|---|
| [exceptionOrNull](../../exception-or-null.html) | [androidJvm]<br>fun &lt;[E](../../exception-or-null.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult.Failure](index.html)&lt;[E](../../exception-or-null.html)&gt;.[exceptionOrNull](../../exception-or-null.html)(): [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html)?<br>Returns the encapsulated [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-throwable/index.html) exception if this is a failure. |
| [withTags](with-tags.html) | [androidJvm]<br>fun [withTags](with-tags.html)(tags: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;): [ClerkApiResult.Failure](index.html)&lt;[E](index.html)&gt; |

