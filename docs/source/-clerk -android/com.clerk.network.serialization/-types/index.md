---
title: Types
---
//[Clerk Android](../../../index.html)/[com.clerk.network.serialization](../index.html)/[Types](index.html)



# Types



[androidJvm]\
object [Types](index.html)

Factory methods for types.



## Functions


| Name | Summary |
|---|---|
| [arrayOf](array-of.html) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [arrayOf](array-of.html)(componentType: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)): [GenericArrayType](https://developer.android.com/reference/kotlin/java/lang/reflect/GenericArrayType.html)<br>Returns an array type whose elements are all instances of `componentType`. |
| [equals](equals.html) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [equals](equals.html)(a: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)?, b: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)?): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Returns true if `a` and `b` are equal. |
| [getRawType](get-raw-type.html) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [getRawType](get-raw-type.html)(type: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)?): [Class](https://developer.android.com/reference/kotlin/java/lang/Class.html)&lt;*&gt; |
| [newParameterizedType](new-parameterized-type.html) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [newParameterizedType](new-parameterized-type.html)(rawType: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html), vararg typeArguments: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)): [ParameterizedType](https://developer.android.com/reference/kotlin/java/lang/reflect/ParameterizedType.html)<br>Returns a new parameterized type, applying `typeArguments` to `rawType`. Use this method if `rawType` is not enclosed in another type. |
| [newParameterizedTypeWithOwner](new-parameterized-type-with-owner.html) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [newParameterizedTypeWithOwner](new-parameterized-type-with-owner.html)(ownerType: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)?, rawType: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html), vararg typeArguments: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)): [ParameterizedType](https://developer.android.com/reference/kotlin/java/lang/reflect/ParameterizedType.html)<br>Returns a new parameterized type, applying `typeArguments` to `rawType`. Use this method if `rawType` is enclosed in `ownerType`. |
| [subtypeOf](subtype-of.html) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [subtypeOf](subtype-of.html)(bound: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)): [WildcardType](https://developer.android.com/reference/kotlin/java/lang/reflect/WildcardType.html)<br>Returns a type that represents an unknown type that extends `bound`. For example, if `bound` is `CharSequence.class`, this returns `? extends CharSequence`. If `bound` is `Object.class`, this returns `?`, which is shorthand for `? extends Object`. |
| [supertypeOf](supertype-of.html) | [androidJvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [supertypeOf](supertype-of.html)(bound: [Type](https://developer.android.com/reference/kotlin/java/lang/reflect/Type.html)): [WildcardType](https://developer.android.com/reference/kotlin/java/lang/reflect/WildcardType.html)<br>Returns a type that represents an unknown supertype of `bound`. For example, if `bound` is `String.class`, this returns `? super String`. |

