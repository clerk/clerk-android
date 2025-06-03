---
title: Passkey
---
//[Clerk Android](../../../index.html)/[com.clerk.model.passkey](../index.html)/[Passkey](index.html)



# Passkey



[androidJvm]\
@Serializable



data class [Passkey](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val verification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, val createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val lastUsedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null)

An object that represents a passkey associated with a user.



## Constructors


| | |
|---|---|
| [Passkey](-passkey.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), verification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), lastUsedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null) |


## Properties


| Name | Summary |
|---|---|
| [createdAt](created-at.html) | [androidJvm]<br>val [createdAt](created-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The date when the passkey was created. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The unique identifier of the passkey. |
| [lastUsedAt](last-used-at.html) | [androidJvm]<br>val [lastUsedAt](last-used-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null<br>The date when the passkey was last used. |
| [name](name.html) | [androidJvm]<br>val [name](name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The passkey's name. |
| [updatedAt](updated-at.html) | [androidJvm]<br>val [updatedAt](updated-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The date when the passkey was last updated. |
| [verification](verification.html) | [androidJvm]<br>val [verification](verification.html): [Verification](../../com.clerk.model.verification/-verification/index.html)? = null<br>The verification details for the passkey. |

