---
title: Success
---
//[Clerk Android](../../../../index.html)/[com.clerk.network.serialization](../../index.html)/[ClerkApiResult](../index.html)/[Success](index.html)



# Success



[androidJvm]\
class [Success](index.html)&lt;out [T](index.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;(val value: [T](index.html), tags: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;) : [ClerkApiResult](../index.html)&lt;[T](index.html), [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-nothing/index.html)&gt; 

A successful result with the data available in [value](value.html).



## Constructors


| | |
|---|---|
| [Success](-success.html) | [androidJvm]<br>constructor(value: [T](index.html), tags: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;) |


## Properties


| Name | Summary |
|---|---|
| [tags](tags.html) | [androidJvm]<br>val [tags](tags.html): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; |
| [value](value.html) | [androidJvm]<br>val [value](value.html): [T](index.html) |


## Functions


| Name | Summary |
|---|---|
| [withTags](with-tags.html) | [androidJvm]<br>fun [withTags](with-tags.html)(tags: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;): [ClerkApiResult.Success](index.html)&lt;[T](index.html)&gt; |

