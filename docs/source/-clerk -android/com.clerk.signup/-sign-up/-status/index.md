---
title: Status
---
//[Clerk Android](../../../../index.html)/[com.clerk.signup](../../index.html)/[SignUp](../index.html)/[Status](index.html)



# Status



[androidJvm]\
@Serializable



enum [Status](index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[SignUp.Status](index.html)&gt; 

Represents the current status of the sign-up process.



The Status enum defines the possible states of a sign-up flow. Each state indicates a specific requirement or completion level in the sign-up process.



## Entries


| | |
|---|---|
| [ABANDONED](-a-b-a-n-d-o-n-e-d/index.html) | [androidJvm]<br>@SerialName(value = &quot;abandoned&quot;)<br>[ABANDONED](-a-b-a-n-d-o-n-e-d/index.html)<br>The sign-up has been inactive for over 24 hours. |
| [MISSING_REQUIREMENTS](-m-i-s-s-i-n-g_-r-e-q-u-i-r-e-m-e-n-t-s/index.html) | [androidJvm]<br>@SerialName(value = &quot;missing_requirements&quot;)<br>[MISSING_REQUIREMENTS](-m-i-s-s-i-n-g_-r-e-q-u-i-r-e-m-e-n-t-s/index.html)<br>A requirement is unverified or missing from the Email, Phone, Username settings. For example, in the Clerk Dashboard, the Password setting is required but a password wasn't provided in the custom flow. |
| [COMPLETE](-c-o-m-p-l-e-t-e/index.html) | [androidJvm]<br>@SerialName(value = &quot;complete&quot;)<br>[COMPLETE](-c-o-m-p-l-e-t-e/index.html)<br>All the required fields have been supplied and verified, so the sign-up is complete and a new user and a session have been created. |
| [UNKNOWN](-u-n-k-n-o-w-n/index.html) | [androidJvm]<br>@SerialName(value = &quot;unknown&quot;)<br>[UNKNOWN](-u-n-k-n-o-w-n/index.html)<br>The status is unknown. |


## Properties


| Name | Summary |
|---|---|
| [entries](entries.html) | [androidJvm]<br>val [entries](entries.html): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[SignUp.Status](index.html)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |


## Functions


| Name | Summary |
|---|---|
| [valueOf](value-of.html) | [androidJvm]<br>fun [valueOf](value-of.html)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [SignUp.Status](index.html)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.html) | [androidJvm]<br>fun [values](values.html)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[SignUp.Status](index.html)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |

