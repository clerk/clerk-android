---
title: GoogleOneTap
---
//[Clerk Android](../../../../../../index.html)/[com.clerk.signin](../../../../index.html)/[SignIn](../../../index.html)/[SignInCreateParams](../../index.html)/[Strategy](../index.html)/[GoogleOneTap](index.html)



# GoogleOneTap





@Serializable



data class [GoogleOneTap](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = GOOGLE_ONE_TAP, val identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.SignInCreateParams.Strategy](../index.html)

Google one tap sign-in strategy.



This strategy is used for Google one tap authentication.



#### Parameters


androidJvm

| | |
|---|---|
| strategy | The strategy identifier for Google one tap sign-in. |
| identifier | The identifier for the Google one tap sign-in. |



## Constructors


| | |
|---|---|
| [GoogleOneTap](-google-one-tap.html) | [androidJvm]<br>constructor(identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = GOOGLE_ONE_TAP, identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [identifier](identifier.html) | [androidJvm]<br>val [identifier](identifier.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [strategy](strategy.html) | [androidJvm]<br>open override val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |

