---
title: Session
---
//[Clerk Android](../../../index.html)/[com.clerk.model.session](../index.html)/[Session](index.html)



# Session



[androidJvm]\
@Serializable



data class [Session](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val status: [Session.SessionStatus](-session-status/index.html), val expireAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val abandonAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val lastActiveAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val latestActivity: [SessionActivity](../-session-activity/index.html)? = null, val lastActiveOrganizationId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val actor: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val user: [User](../../com.clerk.model.user/-user/index.html)? = null, val publicUserData: [PublicUserData](../../com.clerk.model.userdata/-public-user-data/index.html)? = null, val createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val lastActiveToken: [TokenResource](../../com.clerk.model.token/-token-resource/index.html)? = null)

The Session object is an abstraction over an HTTP session. It models the period of information exchange between a user and the server.



The Session object includes methods for recording session activity and ending the session client-side. For security reasons, sessions can also expire server-side.



As soon as a User signs in, Clerk creates a Session for the current Client. Clients can have more than one sessions at any point in time, but only one of those sessions will be active.



In certain scenarios, a session might be replaced by another one. This is often the case with multi-session applications.



All sessions that are expired, removed, replaced, ended or abandoned are not considered valid.



The SessionWithActivities object is a modified Session object. It contains most of the information that the Session object stores, adding extra information about the current session's latest activity.



The additional data included in the latest activity are useful for analytics purposes. A SessionActivity object will provide information about the user's location, device and browser.



While the SessionWithActivities object wraps the most important information around a Session object, the two objects have entirely different methods.



## Constructors


| | |
|---|---|
| [Session](-session.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), status: [Session.SessionStatus](-session-status/index.html), expireAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), abandonAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), lastActiveAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), latestActivity: [SessionActivity](../-session-activity/index.html)? = null, lastActiveOrganizationId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, actor: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, user: [User](../../com.clerk.model.user/-user/index.html)? = null, publicUserData: [PublicUserData](../../com.clerk.model.userdata/-public-user-data/index.html)? = null, createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), lastActiveToken: [TokenResource](../../com.clerk.model.token/-token-resource/index.html)? = null) |


## Types


| Name | Summary |
|---|---|
| [SessionStatus](-session-status/index.html) | [androidJvm]<br>@Serializable<br>enum [SessionStatus](-session-status/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[Session.SessionStatus](-session-status/index.html)&gt; <br>Represents the status of a session. |


## Properties


| Name | Summary |
|---|---|
| [abandonAt](abandon-at.html) | [androidJvm]<br>@SerialName(value = &quot;abandon_at&quot;)<br>val [abandonAt](abandon-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The time when the session was abandoned by the user. |
| [actor](actor.html) | [androidJvm]<br>val [actor](actor.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The JWT actor for the session. |
| [createdAt](created-at.html) | [androidJvm]<br>@SerialName(value = &quot;created_at&quot;)<br>val [createdAt](created-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The time the session was created. |
| [expireAt](expire-at.html) | [androidJvm]<br>@SerialName(value = &quot;expire_at&quot;)<br>val [expireAt](expire-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The time the session expires and will cease to be active. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>A unique identifier for the session. |
| [lastActiveAt](last-active-at.html) | [androidJvm]<br>@SerialName(value = &quot;last_active_at&quot;)<br>val [lastActiveAt](last-active-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The time the session was last active on the client. |
| [lastActiveOrganizationId](last-active-organization-id.html) | [androidJvm]<br>@SerialName(value = &quot;last_active_organization_id&quot;)<br>val [lastActiveOrganizationId](last-active-organization-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The last active organization identifier. |
| [lastActiveToken](last-active-token.html) | [androidJvm]<br>@SerialName(value = &quot;last_active_token&quot;)<br>val [lastActiveToken](last-active-token.html): [TokenResource](../../com.clerk.model.token/-token-resource/index.html)? = null<br>The last active token for the session. |
| [latestActivity](latest-activity.html) | [androidJvm]<br>@SerialName(value = &quot;latest_activity&quot;)<br>val [latestActivity](latest-activity.html): [SessionActivity](../-session-activity/index.html)? = null<br>The latest activity associated with the session. |
| [publicUserData](public-user-data.html) | [androidJvm]<br>@SerialName(value = &quot;public_user_data&quot;)<br>val [publicUserData](public-user-data.html): [PublicUserData](../../com.clerk.model.userdata/-public-user-data/index.html)? = null<br>Public information about the user that this session belongs to. |
| [status](status.html) | [androidJvm]<br>val [status](status.html): [Session.SessionStatus](-session-status/index.html)<br>The current state of the session. |
| [updatedAt](updated-at.html) | [androidJvm]<br>@SerialName(value = &quot;updated_at&quot;)<br>val [updatedAt](updated-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The last time the session recorded activity of any kind. |
| [user](user.html) | [androidJvm]<br>val [user](user.html): [User](../../com.clerk.model.user/-user/index.html)? = null<br>The user associated with the session. |


## Functions


| Name | Summary |
|---|---|
| [delete](../delete.html) | [androidJvm]<br>suspend fun [Session](index.html).[delete](../delete.html)()<br>Deletes the current session. |

