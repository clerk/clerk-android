---
title: EmailCode
---
//[Clerk Android](../../../../../index.html)/[com.clerk.signin](../../../index.html)/[SignIn](../../index.html)/[AttemptFirstFactorParams](../index.html)/[EmailCode](index.html)



# EmailCode



[androidJvm]\
@Serializable



data class [EmailCode](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = EMAIL_CODE, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.AttemptFirstFactorParams](../index.html)

Parameters for email code verification strategy.



## Constructors


| | |
|---|---|
| [EmailCode](-email-code.html) | [androidJvm]<br>constructor(code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = EMAIL_CODE, code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [code](code.html) | [androidJvm]<br>val [code](code.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The verification code received via email. |
| [strategy](strategy.html) | [androidJvm]<br>open override val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The [strategy](strategy.html) value depends on the object's identifier value. Each authentication identifier supports different verification strategies. |

