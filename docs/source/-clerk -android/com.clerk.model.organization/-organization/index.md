---
title: Organization
---
//[Clerk Android](../../../index.html)/[com.clerk.model.organization](../index.html)/[Organization](index.html)



# Organization



[androidJvm]\
@Serializable



data class [Organization](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val slug: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val imageUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val hasImage: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val membersCount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null, val pendingInvitationsCount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null, val maxAllowedMemberships: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), val adminDeleteEnabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val publicMetadata: JsonElement? = null)

The Organization object holds information about an organization, as well as methods for managing it.



## Constructors


| | |
|---|---|
| [Organization](-organization.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), slug: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, imageUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), hasImage: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), membersCount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null, pendingInvitationsCount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null, maxAllowedMemberships: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), adminDeleteEnabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), publicMetadata: JsonElement? = null) |


## Properties


| Name | Summary |
|---|---|
| [adminDeleteEnabled](admin-delete-enabled.html) | [androidJvm]<br>val [adminDeleteEnabled](admin-delete-enabled.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A getter boolean to check if the admin of the organization can delete it. |
| [createdAt](created-at.html) | [androidJvm]<br>val [createdAt](created-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The date when the organization was created. |
| [hasImage](has-image.html) | [androidJvm]<br>val [hasImage](has-image.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A getter boolean to check if the organization has an uploaded image. Returns false if Clerk is displaying an avatar for the organization. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The unique identifier of the related organization. |
| [imageUrl](image-url.html) | [androidJvm]<br>val [imageUrl](image-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>Holds the organization logo or default logo. Compatible with Clerk's Image Optimization. |
| [maxAllowedMemberships](max-allowed-memberships.html) | [androidJvm]<br>val [maxAllowedMemberships](max-allowed-memberships.html): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The maximum number of memberships allowed for the organization. |
| [membersCount](members-count.html) | [androidJvm]<br>val [membersCount](members-count.html): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null<br>The number of members the associated organization contains. |
| [name](name.html) | [androidJvm]<br>val [name](name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The name of the related organization. |
| [pendingInvitationsCount](pending-invitations-count.html) | [androidJvm]<br>val [pendingInvitationsCount](pending-invitations-count.html): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)? = null<br>The number of pending invitations to users to join the organization. |
| [publicMetadata](public-metadata.html) | [androidJvm]<br>val [publicMetadata](public-metadata.html): JsonElement? = null<br>Metadata that can be read from the Frontend API and Backend API and can be set only from the Backend API. |
| [slug](slug.html) | [androidJvm]<br>val [slug](slug.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The organization slug. If supplied, it must be unique for the instance. |
| [updatedAt](updated-at.html) | [androidJvm]<br>val [updatedAt](updated-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The date when the organization was last updated. |

