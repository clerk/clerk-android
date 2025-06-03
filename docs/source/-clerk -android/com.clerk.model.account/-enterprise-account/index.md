---
title: EnterpriseAccount
---
//[Clerk Android](../../../index.html)/[com.clerk.model.account](../index.html)/[EnterpriseAccount](index.html)



# EnterpriseAccount



[androidJvm]\
@Serializable



data class [EnterpriseAccount](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val objectType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val protocol: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val provider: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val active: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val providerUserId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val publicMetadata: JsonObject, val verification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, val enterpriseConnection: [EnterpriseAccount.EnterpriseConnection](-enterprise-connection/index.html))

A model representing an enterprise account.



`EnterpriseAccount` encapsulates the details of a user's enterprise account.



## Constructors


| | |
|---|---|
| [EnterpriseAccount](-enterprise-account.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), objectType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), protocol: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), provider: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), active: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, providerUserId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, publicMetadata: JsonObject, verification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, enterpriseConnection: [EnterpriseAccount.EnterpriseConnection](-enterprise-connection/index.html)) |


## Types


| Name | Summary |
|---|---|
| [EnterpriseConnection](-enterprise-connection/index.html) | [androidJvm]<br>@Serializable<br>data class [EnterpriseConnection](-enterprise-connection/index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val protocol: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val provider: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val logoPublicUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val domain: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val active: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val syncUserAttributes: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val disableAdditionalIdentifications: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val allowSubdomains: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val allowIdpInitiated: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>A model representing the connection details for an enterprise account. |


## Properties


| Name | Summary |
|---|---|
| [active](active.html) | [androidJvm]<br>val [active](active.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A flag indicating whether the enterprise account is active. |
| [emailAddress](email-address.html) | [androidJvm]<br>val [emailAddress](email-address.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The email address associated with the enterprise account. |
| [enterpriseConnection](enterprise-connection.html) | [androidJvm]<br>val [enterpriseConnection](enterprise-connection.html): [EnterpriseAccount.EnterpriseConnection](-enterprise-connection/index.html)<br>Details about the enterprise connection associated with this account. |
| [firstName](first-name.html) | [androidJvm]<br>val [firstName](first-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The first name of the account holder, if available. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The unique identifier for the enterprise account. |
| [lastName](last-name.html) | [androidJvm]<br>val [lastName](last-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The last name of the account holder, if available. |
| [objectType](object-type.html) | [androidJvm]<br>@SerialName(value = &quot;object&quot;)<br>val [objectType](object-type.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The type of object, typically a string identifier indicating the object type. |
| [protocol](protocol.html) | [androidJvm]<br>val [protocol](protocol.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The authentication protocol used (e.g., SAML, OpenID). |
| [provider](provider.html) | [androidJvm]<br>val [provider](provider.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The name of the provider (e.g., Okta, Google). |
| [providerUserId](provider-user-id.html) | [androidJvm]<br>val [providerUserId](provider-user-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The unique user identifier assigned by the provider, if available. |
| [publicMetadata](public-metadata.html) | [androidJvm]<br>val [publicMetadata](public-metadata.html): JsonObject<br>Public metadata associated with the enterprise account. |
| [verification](verification.html) | [androidJvm]<br>val [verification](verification.html): [Verification](../../com.clerk.model.verification/-verification/index.html)? = null<br>Verification information for the enterprise account, if available. |

