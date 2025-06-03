---
title: com.clerk.model.session
---
//[Clerk Android](../../index.html)/[com.clerk.model.session](index.html)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [Session](-session/index.html) | [androidJvm]<br>@Serializable<br>data class [Session](-session/index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val status: [Session.SessionStatus](-session/-session-status/index.html), val expireAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val abandonAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val lastActiveAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val latestActivity: [SessionActivity](-session-activity/index.html)? = null, val lastActiveOrganizationId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val actor: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val user: [User](../com.clerk.model.user/-user/index.html)? = null, val publicUserData: [PublicUserData](../com.clerk.model.userdata/-public-user-data/index.html)? = null, val createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val lastActiveToken: [TokenResource](../com.clerk.model.token/-token-resource/index.html)? = null)<br>The Session object is an abstraction over an HTTP session. It models the period of information exchange between a user and the server. |
| [SessionActivity](-session-activity/index.html) | [androidJvm]<br>@Serializable<br>data class [SessionActivity](-session-activity/index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val browserName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val browserVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val deviceType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val ipAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val city: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val country: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val isMobile: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null)<br>A `SessionActivity` object will provide information about the user's location, device and browser. |


## Functions


| Name | Summary |
|---|---|
| [delete](delete.html) | [androidJvm]<br>suspend fun [Session](-session/index.html).[delete](delete.html)()<br>Deletes the current session. |

