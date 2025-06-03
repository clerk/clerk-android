---
title: PhoneNumber
---
//[Clerk Android](../../../index.html)/[com.clerk.model.phonenumber](../index.html)/[PhoneNumber](index.html)



# PhoneNumber



[androidJvm]\
@Serializable



data class [PhoneNumber](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val phoneNumber: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val verification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, val reservedForSecondFactor: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val defaultSecondFactor: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val linkedTo: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null, val backupCodes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null)

The `PhoneNumber` object is a model around a phone number entity.



Phone numbers can be used as a proof of identification for users, or simply as a means of contacting users.



Phone numbers must be verified to ensure that they can be assigned to their rightful owners. The `PhoneNumber` object holds all the necessary state around the verification process.



- 
   The verification process always starts with the `prepareVerification()` method, which will send a one-time verification code via an SMS message.
- 
   The second and final step involves an attempt to complete the verification by calling the `attemptVerification(code:)` method, passing the one-time code as a parameter.




Finally, phone numbers can be used as part of multi-factor authentication. During sign-in, users can opt in to an extra verification step where they will receive an SMS message with a one-time code. This code must be entered to complete the sign-in process.



## Constructors


| | |
|---|---|
| [PhoneNumber](-phone-number.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), phoneNumber: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), verification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, reservedForSecondFactor: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), defaultSecondFactor: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), linkedTo: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null, backupCodes: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null) |


## Properties


| Name | Summary |
|---|---|
| [backupCodes](backup-codes.html) | [androidJvm]<br>val [backupCodes](backup-codes.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null<br>A list of backup codes in case of lost phone number access. |
| [createdAt](created-at.html) | [androidJvm]<br>val [createdAt](created-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The date when the phone number was created. |
| [defaultSecondFactor](default-second-factor.html) | [androidJvm]<br>val [defaultSecondFactor](default-second-factor.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A boolean indicating whether this phone number is the default second factor. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The unique identifier for this phone number. |
| [linkedTo](linked-to.html) | [androidJvm]<br>val [linkedTo](linked-to.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null<br>An object containing information about any other identification that might be linked to this phone number. |
| [phoneNumber](phone-number.html) | [androidJvm]<br>val [phoneNumber](phone-number.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The phone number value. |
| [reservedForSecondFactor](reserved-for-second-factor.html) | [androidJvm]<br>val [reservedForSecondFactor](reserved-for-second-factor.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A boolean indicating whether this phone number is reserved for second factor authentication. |
| [updatedAt](updated-at.html) | [androidJvm]<br>val [updatedAt](updated-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The date when the phone number was last updated. |
| [verification](verification.html) | [androidJvm]<br>val [verification](verification.html): [Verification](../../com.clerk.model.verification/-verification/index.html)? = null<br>An object holding information on the verification of this phone number. |

