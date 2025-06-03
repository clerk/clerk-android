---
title: supertypeOf
---
//[Clerk Android](../../../index.html)/[com.clerk.network.serialization](../index.html)/[Types](index.html)/[supertypeOf](supertype-of.html)



# supertypeOf



[androidJvm]\




@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)



fun [supertypeOf](supertype-of.html)(bound: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)): [WildcardType](https://developer.android.com/reference/kotlin/java/lang/reflect/WildcardType.html)



Returns a type that represents an unknown supertype of `bound`. For example, if `bound` is `String.class`, this returns `? super String`.




