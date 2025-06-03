---
title: AttemptFirstFactorParams
---
//[Clerk Android](../../../../index.html)/[com.clerk.signin](../../index.html)/[SignIn](../index.html)/[AttemptFirstFactorParams](index.html)



# AttemptFirstFactorParams

sealed interface [AttemptFirstFactorParams](index.html)

A sealed interface defining parameter objects for attempting first factor verification in the sign-in process.



Each implementation represents a different verification strategy that can be used to complete the first factor authentication step.



#### Inheritors


| |
|---|
| [EmailCode](-email-code/index.html) |
| [PhoneCode](-phone-code/index.html) |
| [Password](-password/index.html) |
| [Passkey](-passkey/index.html) |
| [ResetPasswordEmailCode](-reset-password-email-code/index.html) |
| [ResetPasswordPhoneCode](-reset-password-phone-code/index.html) |


## Types


| Name | Summary |
|---|---|
| [EmailCode](-email-code/index.html) | [androidJvm]<br>@Serializable<br>data class [EmailCode](-email-code/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = EMAIL_CODE, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.AttemptFirstFactorParams](index.html)<br>Parameters for email code verification strategy. |
| [Passkey](-passkey/index.html) | [androidJvm]<br>@Serializable<br>data class [Passkey](-passkey/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PASSKEY, val passkey: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.AttemptFirstFactorParams](index.html)<br>Parameters for passkey verification strategy. |
| [Password](-password/index.html) | [androidJvm]<br>@Serializable<br>data class [Password](-password/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PASSWORD, val password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.AttemptFirstFactorParams](index.html)<br>Parameters for password verification strategy. |
| [PhoneCode](-phone-code/index.html) | [androidJvm]<br>@Serializable<br>data class [PhoneCode](-phone-code/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PHONE_CODE, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.AttemptFirstFactorParams](index.html)<br>Parameters for phone code verification strategy. |
| [ResetPasswordEmailCode](-reset-password-email-code/index.html) | [androidJvm]<br>@Serializable<br>data class [ResetPasswordEmailCode](-reset-password-email-code/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = RESET_PASSWORD_EMAIL_CODE, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.AttemptFirstFactorParams](index.html)<br>Parameters for reset password email code verification strategy. |
| [ResetPasswordPhoneCode](-reset-password-phone-code/index.html) | [androidJvm]<br>@Serializable<br>data class [ResetPasswordPhoneCode](-reset-password-phone-code/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = RESET_PASSWORD_PHONE_CODE, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.AttemptFirstFactorParams](index.html)<br>Parameters for reset password phone code verification strategy. |


## Properties


| Name | Summary |
|---|---|
| [strategy](strategy.html) | [androidJvm]<br>abstract val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The [strategy](strategy.html) value depends on the object's identifier value. Each authentication identifier supports different verification strategies. |

