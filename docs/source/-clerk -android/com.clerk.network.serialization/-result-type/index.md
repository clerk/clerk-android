---
title: ResultType
---
//[Clerk Android](../../../index.html)/[com.clerk.network.serialization](../index.html)/[ResultType](index.html)



# ResultType



[androidJvm]\
annotation class [ResultType](index.html)(val rawType: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt;, val typeArgs: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[ResultType](index.html)&gt; = [], val ownerType: [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt; = Nothing::class, val isArray: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Represents a [java.lang.reflect.Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html) via its components. Retrieve it from Retrofit annotations via errorType and piece this back into a real instance via `ResultType.toType()`.



This API should be considered read-only.



## Properties


| Name | Summary |
|---|---|
| [isArray](is-array.html) | [androidJvm]<br>val [isArray](is-array.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [ownerType](owner-type.html) | [androidJvm]<br>val [ownerType](owner-type.html): [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt; |
| [rawType](raw-type.html) | [androidJvm]<br>val [rawType](raw-type.html): [KClass](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.reflect/-k-class/index.html)&lt;*&gt; |
| [typeArgs](type-args.html) | [androidJvm]<br>val [typeArgs](type-args.html): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[ResultType](index.html)&gt; |

