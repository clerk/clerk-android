---
title: PhoneCode
---
//[Clerk Android](../../../../../index.html)/[com.clerk.signup](../../../index.html)/[SignUp](../../index.html)/[AttemptVerificationParams](../index.html)/[PhoneCode](index.html)



# PhoneCode

data class [PhoneCode](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PHONE_CODE, val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignUp.AttemptVerificationParams](../index.html)

Attempts verification using a code sent to the user's phone number.



#### Parameters


androidJvm

| | |
|---|---|
| code | The one-time code sent to the user's phone number. |



## Constructors


| | |
|---|---|
| [PhoneCode](-phone-code.html) | [androidJvm]<br>constructor(code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = PHONE_CODE, code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [code](code.html) | [androidJvm]<br>open override val [code](code.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [strategy](strategy.html) | [androidJvm]<br>open override val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The strategy used for verification (e.g., `email_code` or `phone_code`). |

