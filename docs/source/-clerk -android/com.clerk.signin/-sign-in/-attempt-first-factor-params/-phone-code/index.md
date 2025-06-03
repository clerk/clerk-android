---
title: PhoneCode
---
//[Clerk Android](../../../../../index.html)/[com.clerk.signin](../../../index.html)/[SignIn](../../index.html)/[AttemptFirstFactorParams](../index.html)/[PhoneCode](index.html)



# PhoneCode



[androidJvm]\
@Serializable



data class [PhoneCode](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PHONE_CODE, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.AttemptFirstFactorParams](../index.html)

Parameters for phone code verification strategy.



## Constructors


| | |
|---|---|
| [PhoneCode](-phone-code.html) | [androidJvm]<br>constructor(code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PHONE_CODE, code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [code](code.html) | [androidJvm]<br>val [code](code.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The verification code received via SMS. |
| [strategy](strategy.html) | [androidJvm]<br>open override val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The [strategy](strategy.html) value depends on the object's identifier value. Each authentication identifier supports different verification strategies. |

