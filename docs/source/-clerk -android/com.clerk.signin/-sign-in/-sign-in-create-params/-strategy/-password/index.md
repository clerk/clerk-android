---
title: Password
---
//[Clerk Android](../../../../../../index.html)/[com.clerk.signin](../../../../index.html)/[SignIn](../../../index.html)/[SignInCreateParams](../../index.html)/[Strategy](../index.html)/[Password](index.html)



# Password



[androidJvm]\
@Serializable



data class [Password](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PASSWORD, val identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.SignInCreateParams.Strategy](../index.html)

Password sign-in strategy.



## Constructors


| | |
|---|---|
| [Password](-password.html) | [androidJvm]<br>constructor(identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PASSWORD, identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [identifier](identifier.html) | [androidJvm]<br>val [identifier](identifier.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The email address or username for password authentication. |
| [strategy](strategy.html) | [androidJvm]<br>open override val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The authentication strategy identifier. |

