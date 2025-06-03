---
title: AttemptVerificationParams
---
//[Clerk Android](../../../../index.html)/[com.clerk.signup](../../index.html)/[SignUp](../index.html)/[AttemptVerificationParams](index.html)



# AttemptVerificationParams

sealed interface [AttemptVerificationParams](index.html)

Defines the possible strategies for attempting verification during the sign-up process. This sealed interface encapsulates the different types of verification attempts, such as email or phone code verification.



#### Inheritors


| |
|---|
| [EmailCode](-email-code/index.html) |
| [PhoneCode](-phone-code/index.html) |


## Types


| Name | Summary |
|---|---|
| [EmailCode](-email-code/index.html) | [androidJvm]<br>data class [EmailCode](-email-code/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = EMAIL_CODE, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignUp.AttemptVerificationParams](index.html)<br>Attempts verification using a code sent to the user's email address. |
| [PhoneCode](-phone-code/index.html) | [androidJvm]<br>data class [PhoneCode](-phone-code/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PHONE_CODE, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignUp.AttemptVerificationParams](index.html)<br>Attempts verification using a code sent to the user's phone number. |


## Properties


| Name | Summary |
|---|---|
| [code](code.html) | [androidJvm]<br>abstract val [code](code.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The verification code provided by the user. |
| [strategy](strategy.html) | [androidJvm]<br>abstract val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The strategy used for verification (e.g., `email_code` or `phone_code`). |

