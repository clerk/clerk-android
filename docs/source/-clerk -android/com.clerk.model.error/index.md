---
title: com.clerk.model.error
---
//[Clerk Android](../../index.html)/[com.clerk.model.error](index.html)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [ClerkErrorResponse](-clerk-error-response/index.html) | [androidJvm]<br>@Serializable<br>data class [ClerkErrorResponse](-clerk-error-response/index.html)(val errors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Error](-error/index.html)&gt;, val meta: [Meta](-meta/index.html)? = null, val clerkTraceId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))<br>Represents an error response from the Clerk API. |
| [Error](-error/index.html) | [androidJvm]<br>@Serializable<br>data class [Error](-error/index.html)(val message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val longMessage: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |
| [Meta](-meta/index.html) | [androidJvm]<br>@Serializable<br>data class [Meta](-meta/index.html)(val client: [Client](../com.clerk.model.client/-client/index.html)? = null) |

