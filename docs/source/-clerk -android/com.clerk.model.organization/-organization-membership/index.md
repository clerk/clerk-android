---
title: OrganizationMembership
---
//[Clerk Android](../../../index.html)/[com.clerk.model.organization](../index.html)/[OrganizationMembership](index.html)



# OrganizationMembership



[androidJvm]\
@Serializable



data class [OrganizationMembership](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val publicMetadata: JsonElement, val role: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val permissions: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null, val publicUserData: [PublicUserData](../../com.clerk.model.userdata/-public-user-data/index.html)? = null, val organization: [Organization](../-organization/index.html), val createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), val updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html))

The `OrganizationMembership` object is the model around an organization membership entity and describes the relationship between users and organizations.



## Constructors


| | |
|---|---|
| [OrganizationMembership](-organization-membership.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), publicMetadata: JsonElement, role: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), permissions: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null, publicUserData: [PublicUserData](../../com.clerk.model.userdata/-public-user-data/index.html)? = null, organization: [Organization](../-organization/index.html), createdAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html), updatedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [createdAt](created-at.html) | [androidJvm]<br>val [createdAt](created-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The date when the membership was created. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The unique identifier for this organization membership. |
| [organization](organization.html) | [androidJvm]<br>val [organization](organization.html): [Organization](../-organization/index.html)<br>The `Organization` object the membership belongs to. |
| [permissions](permissions.html) | [androidJvm]<br>val [permissions](permissions.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null<br>The permissions associated with the role. |
| [publicMetadata](public-metadata.html) | [androidJvm]<br>val [publicMetadata](public-metadata.html): JsonElement<br>Metadata that can be read from the Frontend API and Backend API and can be set only from the Backend API. |
| [publicUserData](public-user-data.html) | [androidJvm]<br>val [publicUserData](public-user-data.html): [PublicUserData](../../com.clerk.model.userdata/-public-user-data/index.html)? = null<br>Public information about the user that this membership belongs to. |
| [role](role.html) | [androidJvm]<br>val [role](role.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The role of the current user in the organization. |
| [updatedAt](updated-at.html) | [androidJvm]<br>val [updatedAt](updated-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)<br>The date when the membership was last updated. |

