---
title: Strategy
---
//[Clerk Android](../../../../../index.html)/[com.clerk.signin](../../../index.html)/[SignIn](../../index.html)/[SignInCreateParams](../index.html)/[Strategy](index.html)



# Strategy

sealed interface [Strategy](index.html)

A sealed interface defining different strategies for creating a sign-in.



Each implementation represents a different method of initiating the sign-in process.



#### Inheritors


| |
|---|
| [EmailCode](-email-code/index.html) |
| [PhoneCode](-phone-code/index.html) |
| [Password](-password/index.html) |
| [Transfer](-transfer/index.html) |
| [GoogleOneTap](-google-one-tap/index.html) |
| [Ticket](-ticket/index.html) |


## Types


| Name | Summary |
|---|---|
| [EmailCode](-email-code/index.html) | [androidJvm]<br>@Serializable<br>data class [EmailCode](-email-code/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = EMAIL_CODE, val identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.SignInCreateParams.Strategy](index.html)<br>Email code sign-in strategy. |
| [GoogleOneTap](-google-one-tap/index.html) | [androidJvm]<br>@Serializable<br>data class [GoogleOneTap](-google-one-tap/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = GOOGLE_ONE_TAP, val identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.SignInCreateParams.Strategy](index.html)<br>Google one tap sign-in strategy. |
| [Password](-password/index.html) | [androidJvm]<br>@Serializable<br>data class [Password](-password/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PASSWORD, val identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.SignInCreateParams.Strategy](index.html)<br>Password sign-in strategy. |
| [PhoneCode](-phone-code/index.html) | [androidJvm]<br>@Serializable<br>data class [PhoneCode](-phone-code/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PHONE_CODE, val identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.SignInCreateParams.Strategy](index.html)<br>Phone code sign-in strategy. |
| [Ticket](-ticket/index.html) | [androidJvm]<br>@Serializable<br>data class [Ticket](-ticket/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = TICKET, val ticket: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.SignInCreateParams.Strategy](index.html)<br>Ticket strategy for authentication using a ticket. |
| [Transfer](-transfer/index.html) | [androidJvm]<br>data class [Transfer](-transfer/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = TRANSFER) : [SignIn.SignInCreateParams.Strategy](index.html)<br>Transfer strategy for account transfer scenarios. |


## Properties


| Name | Summary |
|---|---|
| [strategy](strategy.html) | [androidJvm]<br>abstract val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The authentication strategy identifier. |

