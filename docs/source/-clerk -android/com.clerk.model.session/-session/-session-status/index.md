---
title: SessionStatus
---
//[Clerk Android](../../../../index.html)/[com.clerk.model.session](../../index.html)/[Session](../index.html)/[SessionStatus](index.html)



# SessionStatus



[androidJvm]\
@Serializable



enum [SessionStatus](index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[Session.SessionStatus](index.html)&gt; 

Represents the status of a session.



## Entries


| | |
|---|---|
| [ABANDONED](-a-b-a-n-d-o-n-e-d/index.html) | [androidJvm]<br>@SerialName(value = &quot;abandoned&quot;)<br>[ABANDONED](-a-b-a-n-d-o-n-e-d/index.html)<br>The session was abandoned client-side. |
| [ACTIVE](-a-c-t-i-v-e/index.html) | [androidJvm]<br>@SerialName(value = &quot;active&quot;)<br>[ACTIVE](-a-c-t-i-v-e/index.html)<br>The session is valid, and all activity is allowed. |
| [ENDED](-e-n-d-e-d/index.html) | [androidJvm]<br>@SerialName(value = &quot;ended&quot;)<br>[ENDED](-e-n-d-e-d/index.html)<br>The user signed out of the session, but the Session remains in the Client object. |
| [EXPIRED](-e-x-p-i-r-e-d/index.html) | [androidJvm]<br>@SerialName(value = &quot;expired&quot;)<br>[EXPIRED](-e-x-p-i-r-e-d/index.html)<br>The period of allowed activity for this session has passed. |
| [REMOVED](-r-e-m-o-v-e-d/index.html) | [androidJvm]<br>@SerialName(value = &quot;removed&quot;)<br>[REMOVED](-r-e-m-o-v-e-d/index.html)<br>The user signed out of the session, and the Session was removed from the Client object. |
| [REPLACED](-r-e-p-l-a-c-e-d/index.html) | [androidJvm]<br>@SerialName(value = &quot;replaced&quot;)<br>[REPLACED](-r-e-p-l-a-c-e-d/index.html)<br>The session has been replaced by another one, but the Session remains in the Client object. |
| [REVOKED](-r-e-v-o-k-e-d/index.html) | [androidJvm]<br>@SerialName(value = &quot;revoked&quot;)<br>[REVOKED](-r-e-v-o-k-e-d/index.html)<br>The application ended the session, and the Session was removed from the Client object. |
| [UNKNOWN](-u-n-k-n-o-w-n/index.html) | [androidJvm]<br>@SerialName(value = &quot;unknown&quot;)<br>[UNKNOWN](-u-n-k-n-o-w-n/index.html)<br>Unknown session status. |


## Properties


| Name | Summary |
|---|---|
| [entries](entries.html) | [androidJvm]<br>val [entries](entries.html): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[Session.SessionStatus](index.html)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |


## Functions


| Name | Summary |
|---|---|
| [valueOf](value-of.html) | [androidJvm]<br>fun [valueOf](value-of.html)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [Session.SessionStatus](index.html)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.html) | [androidJvm]<br>fun [values](values.html)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Session.SessionStatus](index.html)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |

