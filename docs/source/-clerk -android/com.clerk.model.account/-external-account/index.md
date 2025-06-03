---
title: ExternalAccount
---
//[Clerk Android](../../../index.html)/[com.clerk.model.account](../index.html)/[ExternalAccount](index.html)



# ExternalAccount



[androidJvm]\
@Serializable



data class [ExternalAccount](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val identificationId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val provider: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val providerUserId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val approvedScopes: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val imageUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val publicMetadata: JsonObject, val label: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val verification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null)

The `ExternalAccount` object is a model around an identification obtained by an external provider (e.g. a social provider such as Google).



External account must be verified, so that you can make sure they can be assigned to their rightful owners. The `ExternalAccount` object holds all necessary state around the verification process.



## Constructors


| | |
|---|---|
| [ExternalAccount](-external-account.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), identificationId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), provider: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), providerUserId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), approvedScopes: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, imageUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, publicMetadata: JsonObject, label: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, verification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null) |


## Properties


| Name | Summary |
|---|---|
| [approvedScopes](approved-scopes.html) | [androidJvm]<br>val [approvedScopes](approved-scopes.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The scopes that the user has granted access to. |
| [emailAddress](email-address.html) | [androidJvm]<br>val [emailAddress](email-address.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The provided email address of the user. |
| [firstName](first-name.html) | [androidJvm]<br>val [firstName](first-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The user's first name. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The unique identifier for this external account. |
| [identificationId](identification-id.html) | [androidJvm]<br>val [identificationId](identification-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The identification with which this external account is associated. |
| [imageUrl](image-url.html) | [androidJvm]<br>val [imageUrl](image-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The user's image URL. |
| [label](label.html) | [androidJvm]<br>val [label](label.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>A descriptive label to differentiate multiple external accounts of the same user for the same provider. |
| [lastName](last-name.html) | [androidJvm]<br>val [lastName](last-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The user's last name. |
| [provider](provider.html) | [androidJvm]<br>val [provider](provider.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The provider name e.g. google |
| [providerUserId](provider-user-id.html) | [androidJvm]<br>val [providerUserId](provider-user-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The unique ID of the user in the provider. |
| [publicMetadata](public-metadata.html) | [androidJvm]<br>val [publicMetadata](public-metadata.html): JsonObject<br>Metadata that can be read from the Frontend API and Backend API and can be set only from the Backend API. |
| [username](username.html) | [androidJvm]<br>val [username](username.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The user's username. |
| [verification](verification.html) | [androidJvm]<br>val [verification](verification.html): [Verification](../../com.clerk.model.verification/-verification/index.html)? = null<br>An object holding information on the verification of this external account. |

