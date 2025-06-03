---
title: EnterpriseConnection
---
//[Clerk Android](../../../../index.html)/[com.clerk.model.account](../../index.html)/[EnterpriseAccount](../index.html)/[EnterpriseConnection](index.html)



# EnterpriseConnection



[androidJvm]\
@Serializable



data class [EnterpriseConnection](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val protocol: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val provider: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val logoPublicUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val domain: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val active: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val syncUserAttributes: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val disableAdditionalIdentifications: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val allowSubdomains: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val allowIdpInitiated: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

A model representing the connection details for an enterprise account.



`EnterpriseConnection` contains the configuration and metadata for the connection between the enterprise account and the identity provider.



## Constructors


| | |
|---|---|
| [EnterpriseConnection](-enterprise-connection.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), protocol: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), provider: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), logoPublicUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), domain: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), active: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), syncUserAttributes: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), disableAdditionalIdentifications: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), allowSubdomains: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), allowIdpInitiated: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [active](active.html) | [androidJvm]<br>val [active](active.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A flag indicating whether the enterprise connection is active. |
| [allowIdpInitiated](allow-idp-initiated.html) | [androidJvm]<br>val [allowIdpInitiated](allow-idp-initiated.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A flag indicating whether IDP-initiated flows are allowed. |
| [allowSubdomains](allow-subdomains.html) | [androidJvm]<br>val [allowSubdomains](allow-subdomains.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A flag indicating whether subdomains are allowed for the enterprise connection. |
| [createdAt](created-at.html) | [androidJvm]<br>val [createdAt](created-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The date and time when the enterprise connection was created. |
| [disableAdditionalIdentifications](disable-additional-identifications.html) | [androidJvm]<br>val [disableAdditionalIdentifications](disable-additional-identifications.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A flag indicating whether additional user identifications are disabled for this connection. |
| [domain](domain.html) | [androidJvm]<br>val [domain](domain.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The domain associated with the enterprise connection (e.g., example.com). |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The unique identifier for the enterprise connection. |
| [logoPublicUrl](logo-public-url.html) | [androidJvm]<br>val [logoPublicUrl](logo-public-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The public URL of the provider's logo. |
| [name](name.html) | [androidJvm]<br>val [name](name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The display name of the enterprise connection. |
| [protocol](protocol.html) | [androidJvm]<br>val [protocol](protocol.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The authentication protocol used (e.g., SAML, OpenID). |
| [provider](provider.html) | [androidJvm]<br>val [provider](provider.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The name of the provider (e.g., Okta, Google Workspace). |
| [syncUserAttributes](sync-user-attributes.html) | [androidJvm]<br>val [syncUserAttributes](sync-user-attributes.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A flag indicating whether user attributes are synchronized with the provider. |
| [updatedAt](updated-at.html) | [androidJvm]<br>val [updatedAt](updated-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The date and time when the enterprise connection was last updated. |

