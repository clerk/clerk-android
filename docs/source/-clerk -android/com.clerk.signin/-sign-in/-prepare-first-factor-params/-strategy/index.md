---
title: Strategy
---
//[Clerk Android](../../../../../index.html)/[com.clerk.signin](../../../index.html)/[SignIn](../../index.html)/[PrepareFirstFactorParams](../index.html)/[Strategy](index.html)



# Strategy



[androidJvm]\
@Serializable



enum [Strategy](index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[SignIn.PrepareFirstFactorParams.Strategy](index.html)&gt; 

Enumeration of available first factor verification strategies.



## Entries


| | |
|---|---|
| [EMAIL_CODE](-e-m-a-i-l_-c-o-d-e/index.html) | [androidJvm]<br>[EMAIL_CODE](-e-m-a-i-l_-c-o-d-e/index.html)<br>Email code verification strategy. |
| [PHONE_CODE](-p-h-o-n-e_-c-o-d-e/index.html) | [androidJvm]<br>[PHONE_CODE](-p-h-o-n-e_-c-o-d-e/index.html)<br>Phone code (SMS) verification strategy. |
| [PASSWORD](-p-a-s-s-w-o-r-d/index.html) | [androidJvm]<br>[PASSWORD](-p-a-s-s-w-o-r-d/index.html)<br>Password verification strategy. |
| [PASSKEY](-p-a-s-s-k-e-y/index.html) | [androidJvm]<br>[PASSKEY](-p-a-s-s-k-e-y/index.html)<br>Passkey verification strategy. |
| [O_AUTH](-o_-a-u-t-h/index.html) | [androidJvm]<br>[O_AUTH](-o_-a-u-t-h/index.html)<br>OAuth verification strategy. |
| [RESET_PASSWORD_EMAIL_CODE](-r-e-s-e-t_-p-a-s-s-w-o-r-d_-e-m-a-i-l_-c-o-d-e/index.html) | [androidJvm]<br>[RESET_PASSWORD_EMAIL_CODE](-r-e-s-e-t_-p-a-s-s-w-o-r-d_-e-m-a-i-l_-c-o-d-e/index.html)<br>Reset password email code verification strategy. |
| [RESET_PASSWORD_PHONE_CODE](-r-e-s-e-t_-p-a-s-s-w-o-r-d_-p-h-o-n-e_-c-o-d-e/index.html) | [androidJvm]<br>[RESET_PASSWORD_PHONE_CODE](-r-e-s-e-t_-p-a-s-s-w-o-r-d_-p-h-o-n-e_-c-o-d-e/index.html)<br>Reset password phone code verification strategy. |


## Properties


| Name | Summary |
|---|---|
| [entries](entries.html) | [androidJvm]<br>val [entries](entries.html): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[SignIn.PrepareFirstFactorParams.Strategy](index.html)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |


## Functions


| Name | Summary |
|---|---|
| [valueOf](value-of.html) | [androidJvm]<br>fun [valueOf](value-of.html)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [SignIn.PrepareFirstFactorParams.Strategy](index.html)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.html) | [androidJvm]<br>fun [values](values.html)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[SignIn.PrepareFirstFactorParams.Strategy](index.html)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |

