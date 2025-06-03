---
title: Verification
---
//[Clerk Android](../../../index.html)/[com.clerk.model.verification](../index.html)/[Verification](index.html)



# Verification



[androidJvm]\
@Serializable



data class [Verification](index.html)(val status: [Verification.Status](-status/index.html)? = null, val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val attempts: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null, val expireAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null, val error: [Error](../../com.clerk.model.error/-error/index.html)? = null, val externalVerificationRedirectUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val nonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null)

The state of the verification process of a sign-in or sign-up attempt.



## Constructors


| | |
|---|---|
| [Verification](-verification.html) | [androidJvm]<br>constructor(status: [Verification.Status](-status/index.html)? = null, strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, attempts: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null, expireAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null, error: [Error](../../com.clerk.model.error/-error/index.html)? = null, externalVerificationRedirectUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, nonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null) |


## Types


| Name | Summary |
|---|---|
| [Status](-status/index.html) | [androidJvm]<br>@Serializable<br>enum [Status](-status/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[Verification.Status](-status/index.html)&gt; <br>The state of the verification. |


## Properties


| Name | Summary |
|---|---|
| [attempts](attempts.html) | [androidJvm]<br>val [attempts](attempts.html): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null<br>The number of attempts related to the verification. |
| [error](error.html) | [androidJvm]<br>val [error](error.html): [Error](../../com.clerk.model.error/-error/index.html)? = null<br>The last error the verification attempt ran into. |
| [expireAt](expire-at.html) | [androidJvm]<br>val [expireAt](expire-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null<br>The time the verification will expire at. |
| [externalVerificationRedirectUrl](external-verification-redirect-url.html) | [androidJvm]<br>val [externalVerificationRedirectUrl](external-verification-redirect-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The redirect URL for an external verification. |
| [nonce](nonce.html) | [androidJvm]<br>val [nonce](nonce.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The nonce pertaining to the verification. |
| [status](status.html) | [androidJvm]<br>val [status](status.html): [Verification.Status](-status/index.html)? = null<br>The state of the verification. |
| [strategy](strategy.html) | [androidJvm]<br>val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The strategy pertaining to the parent sign-up or sign-in attempt. |

