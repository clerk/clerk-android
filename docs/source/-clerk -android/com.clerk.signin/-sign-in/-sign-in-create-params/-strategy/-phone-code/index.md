---
title: PhoneCode
---
//[Clerk Android](../../../../../../index.html)/[com.clerk.signin](../../../../index.html)/[SignIn](../../../index.html)/[SignInCreateParams](../../index.html)/[Strategy](../index.html)/[PhoneCode](index.html)



# PhoneCode



[androidJvm]\
@Serializable



data class [PhoneCode](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PHONE_CODE, val identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.SignInCreateParams.Strategy](../index.html)

Phone code sign-in strategy.



## Constructors


| | |
|---|---|
| [PhoneCode](-phone-code.html) | [androidJvm]<br>constructor(identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PHONE_CODE, identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [identifier](identifier.html) | [androidJvm]<br>val [identifier](identifier.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The phone number to send the verification code to. |
| [strategy](strategy.html) | [androidJvm]<br>open override val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The authentication strategy identifier. |

