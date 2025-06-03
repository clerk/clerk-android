---
title: EmailAddress
---
//[Clerk Android](../../../index.html)/[com.clerk.model.emailaddress](../index.html)/[EmailAddress](index.html)



# EmailAddress



[androidJvm]\
@Serializable



data class [EmailAddress](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val verification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, val linkedTo: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;JsonElement&gt;? = null)

The EmailAddress object represents an email address associated with a user.



## Constructors


| | |
|---|---|
| [EmailAddress](-email-address.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), verification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, linkedTo: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;JsonElement&gt;? = null) |


## Properties


| Name | Summary |
|---|---|
| [emailAddress](email-address.html) | [androidJvm]<br>@SerialName(value = &quot;email_address&quot;)<br>val [emailAddress](email-address.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The email address value. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The unique identifier for the email address. |
| [linkedTo](linked-to.html) | [androidJvm]<br>@SerialName(value = &quot;linked_to&quot;)<br>val [linkedTo](linked-to.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;JsonElement&gt;? = null<br>A list of linked accounts or identifiers associated with this email address. |
| [verification](verification.html) | [androidJvm]<br>val [verification](verification.html): [Verification](../../com.clerk.model.verification/-verification/index.html)? = null<br>The verification status of the email address. |

