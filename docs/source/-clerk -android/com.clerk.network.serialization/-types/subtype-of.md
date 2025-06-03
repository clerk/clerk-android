---
title: subtypeOf
---
//[Clerk Android](../../../index.html)/[com.clerk.network.serialization](../index.html)/[Types](index.html)/[subtypeOf](subtype-of.html)



# subtypeOf



[androidJvm]\




@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)



fun [subtypeOf](subtype-of.html)(bound: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)): [WildcardType](https://developer.android.com/reference/kotlin/java/lang/reflect/WildcardType.html)



Returns a type that represents an unknown type that extends `bound`. For example, if `bound` is `CharSequence.class`, this returns `? extends CharSequence`. If `bound` is `Object.class`, this returns `?`, which is shorthand for `? extends Object`.




