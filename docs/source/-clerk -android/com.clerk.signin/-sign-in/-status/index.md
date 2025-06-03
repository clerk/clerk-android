---
title: Status
---
//[Clerk Android](../../../../index.html)/[com.clerk.signin](../../index.html)/[SignIn](../index.html)/[Status](index.html)



# Status



[androidJvm]\
@Serializable



enum [Status](index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[SignIn.Status](index.html)&gt; 

Represents the status of a sign-in process.



## Entries


| | |
|---|---|
| [COMPLETE](-c-o-m-p-l-e-t-e/index.html) | [androidJvm]<br>@SerialName(value = &quot;complete&quot;)<br>[COMPLETE](-c-o-m-p-l-e-t-e/index.html)<br>The sign-in process is complete. |
| [NEEDS_FIRST_FACTOR](-n-e-e-d-s_-f-i-r-s-t_-f-a-c-t-o-r/index.html) | [androidJvm]<br>@SerialName(value = &quot;needs_first_factor&quot;)<br>[NEEDS_FIRST_FACTOR](-n-e-e-d-s_-f-i-r-s-t_-f-a-c-t-o-r/index.html)<br>The sign-in process needs a first factor verification. |
| [NEEDS_SECOND_FACTOR](-n-e-e-d-s_-s-e-c-o-n-d_-f-a-c-t-o-r/index.html) | [androidJvm]<br>@SerialName(value = &quot;needs_second_factor&quot;)<br>[NEEDS_SECOND_FACTOR](-n-e-e-d-s_-s-e-c-o-n-d_-f-a-c-t-o-r/index.html)<br>The sign-in process needs a second factor verification. |
| [NEEDS_IDENTIFIER](-n-e-e-d-s_-i-d-e-n-t-i-f-i-e-r/index.html) | [androidJvm]<br>@SerialName(value = &quot;needs_identifier&quot;)<br>[NEEDS_IDENTIFIER](-n-e-e-d-s_-i-d-e-n-t-i-f-i-e-r/index.html)<br>The sign-in process needs an identifier. |
| [NEEDS_NEW_PASSWORD](-n-e-e-d-s_-n-e-w_-p-a-s-s-w-o-r-d/index.html) | [androidJvm]<br>@SerialName(value = &quot;needs_new_password&quot;)<br>[NEEDS_NEW_PASSWORD](-n-e-e-d-s_-n-e-w_-p-a-s-s-w-o-r-d/index.html)<br>The user needs to create a new password. |
| [UNKNOWN](-u-n-k-n-o-w-n/index.html) | [androidJvm]<br>[UNKNOWN](-u-n-k-n-o-w-n/index.html)<br>The sign-in process is in an unknown state. |


## Properties


| Name | Summary |
|---|---|
| [entries](entries.html) | [androidJvm]<br>val [entries](entries.html): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[SignIn.Status](index.html)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |


## Functions


| Name | Summary |
|---|---|
| [valueOf](value-of.html) | [androidJvm]<br>fun [valueOf](value-of.html)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [SignIn.Status](index.html)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.html) | [androidJvm]<br>fun [values](values.html)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[SignIn.Status](index.html)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |

