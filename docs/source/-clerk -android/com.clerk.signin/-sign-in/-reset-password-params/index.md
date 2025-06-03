---
title: ResetPasswordParams
---
//[Clerk Android](../../../../index.html)/[com.clerk.signin](../../index.html)/[SignIn](../index.html)/[ResetPasswordParams](index.html)



# ResetPasswordParams



[androidJvm]\
@Serializable



data class [ResetPasswordParams](index.html)(val password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val signOutOfOtherSessions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false)

Parameters for resetting a user's password during the sign-in process.



## Constructors


| | |
|---|---|
| [ResetPasswordParams](-reset-password-params.html) | [androidJvm]<br>constructor(password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), signOutOfOtherSessions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false) |


## Properties


| Name | Summary |
|---|---|
| [password](password.html) | [androidJvm]<br>val [password](password.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The new password to set for the user. |
| [signOutOfOtherSessions](sign-out-of-other-sessions.html) | [androidJvm]<br>@SerialName(value = &quot;sign_out_of_other_sessions&quot;)<br>val [signOutOfOtherSessions](sign-out-of-other-sessions.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false<br>Whether to sign out of all other sessions when the password is reset. |

