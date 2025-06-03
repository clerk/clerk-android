---
title: Client
---
//[Clerk Android](../../../index.html)/[com.clerk.model.client](../index.html)/[Client](index.html)



# Client



[androidJvm]\
@Serializable



data class [Client](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val signIn: [SignIn](../../com.clerk.signin/-sign-in/index.html)? = null, val signUp: [SignUp](../../com.clerk.signup/-sign-up/index.html)? = null, val sessions: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Session](../../com.clerk.model.session/-session/index.html)&gt; = emptyList(), val lastActiveSessionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null)

The Client object keeps track of the authenticated sessions in the current device. The device can be a browser, a native application or any other medium that is usually the requesting part in a request/response architecture.



The Client object also holds information about any sign in or sign up attempts that might be in progress, tracking the sign in or sign up progress.



## Constructors


| | |
|---|---|
| [Client](-client.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, signIn: [SignIn](../../com.clerk.signin/-sign-in/index.html)? = null, signUp: [SignUp](../../com.clerk.signup/-sign-up/index.html)? = null, sessions: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Session](../../com.clerk.model.session/-session/index.html)&gt; = emptyList(), lastActiveSessionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [androidJvm]<br>object [Companion](-companion/index.html) |


## Properties


| Name | Summary |
|---|---|
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>Unique identifier for this client. |
| [lastActiveSessionId](last-active-session-id.html) | [androidJvm]<br>@SerialName(value = &quot;last_active_session_id&quot;)<br>val [lastActiveSessionId](last-active-session-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The ID of the last active Session on this client. |
| [sessions](sessions.html) | [androidJvm]<br>val [sessions](sessions.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Session](../../com.clerk.model.session/-session/index.html)&gt;<br>A list of sessions that have been created on this client. |
| [signIn](sign-in.html) | [androidJvm]<br>@SerialName(value = &quot;sign_in&quot;)<br>val [signIn](sign-in.html): [SignIn](../../com.clerk.signin/-sign-in/index.html)? = null<br>The current sign in attempt, or null if there is none. |
| [signUp](sign-up.html) | [androidJvm]<br>@SerialName(value = &quot;sign_up&quot;)<br>val [signUp](sign-up.html): [SignUp](../../com.clerk.signup/-sign-up/index.html)? = null<br>The current sign up attempt, or null if there is none. |
| [updatedAt](updated-at.html) | [androidJvm]<br>@SerialName(value = &quot;updated_at&quot;)<br>val [updatedAt](updated-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null<br>Timestamp of last update for the client. |

