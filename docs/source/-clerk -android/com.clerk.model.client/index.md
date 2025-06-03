---
title: com.clerk.model.client
---
//[Clerk Android](../../index.html)/[com.clerk.model.client](index.html)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [Client](-client/index.html) | [androidJvm]<br>@Serializable<br>data class [Client](-client/index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val signIn: [SignIn](../com.clerk.signin/-sign-in/index.html)? = null, val signUp: [SignUp](../com.clerk.signup/-sign-up/index.html)? = null, val sessions: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Session](../com.clerk.model.session/-session/index.html)&gt; = emptyList(), val lastActiveSessionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null)<br>The Client object keeps track of the authenticated sessions in the current device. The device can be a browser, a native application or any other medium that is usually the requesting part in a request/response architecture. |

