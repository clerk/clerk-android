---
title: Password
---
//[Clerk Android](../../../../../index.html)/[com.clerk.signin](../../../index.html)/[SignIn](../../index.html)/[AttemptFirstFactorParams](../index.html)/[Password](index.html)



# Password



[androidJvm]\
@Serializable



data class [Password](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PASSWORD, val password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.AttemptFirstFactorParams](../index.html)

Parameters for password verification strategy.



## Constructors


| | |
|---|---|
| [Password](-password.html) | [androidJvm]<br>constructor(password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PASSWORD, password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [password](password.html) | [androidJvm]<br>@SerialName(value = &quot;password&quot;)<br>val [password](password.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The user's password. |
| [strategy](strategy.html) | [androidJvm]<br>open override val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The [strategy](strategy.html) value depends on the object's identifier value. Each authentication identifier supports different verification strategies. |

