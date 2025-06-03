---
title: com.clerk.model.verification
---
//[Clerk Android](../../index.html)/[com.clerk.model.verification](index.html)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [Verification](-verification/index.html) | [androidJvm]<br>@Serializable<br>data class [Verification](-verification/index.html)(val status: [Verification.Status](-verification/-status/index.html)? = null, val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val attempts: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null, val expireAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null, val error: [Error](../com.clerk.model.error/-error/index.html)? = null, val externalVerificationRedirectUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val nonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null)<br>The state of the verification process of a sign-in or sign-up attempt. |

