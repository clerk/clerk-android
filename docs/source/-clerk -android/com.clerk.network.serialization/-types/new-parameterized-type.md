---
title: newParameterizedType
---
//[Clerk Android](../../../index.html)/[com.clerk.network.serialization](../index.html)/[Types](index.html)/[newParameterizedType](new-parameterized-type.html)



# newParameterizedType



[androidJvm]\




@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)



fun [newParameterizedType](new-parameterized-type.html)(rawType: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html), vararg typeArguments: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)): [ParameterizedType](https://developer.android.com/reference/kotlin/java/lang/reflect/ParameterizedType.html)



Returns a new parameterized type, applying `typeArguments` to `rawType`. Use this method if `rawType` is not enclosed in another type.




