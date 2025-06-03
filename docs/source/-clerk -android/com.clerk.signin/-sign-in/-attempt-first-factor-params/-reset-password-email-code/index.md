---
title: ResetPasswordEmailCode
---
//[Clerk Android](../../../../../index.html)/[com.clerk.signin](../../../index.html)/[SignIn](../../index.html)/[AttemptFirstFactorParams](../index.html)/[ResetPasswordEmailCode](index.html)



# ResetPasswordEmailCode



[androidJvm]\
@Serializable



data class [ResetPasswordEmailCode](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = RESET_PASSWORD_EMAIL_CODE, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.AttemptFirstFactorParams](../index.html)

Parameters for reset password email code verification strategy.



## Constructors


| | |
|---|---|
| [ResetPasswordEmailCode](-reset-password-email-code.html) | [androidJvm]<br>constructor(code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = RESET_PASSWORD_EMAIL_CODE, code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [code](code.html) | [androidJvm]<br>val [code](code.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The verification code received via email for password reset. |
| [strategy](strategy.html) | [androidJvm]<br>open override val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The [strategy](strategy.html) value depends on the object's identifier value. Each authentication identifier supports different verification strategies. |

