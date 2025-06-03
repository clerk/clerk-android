---
title: PublicUserData
---
//[Clerk Android](../../../index.html)/[com.clerk.model.userdata](../index.html)/[PublicUserData](index.html)



# PublicUserData



[androidJvm]\
@Serializable



data class [PublicUserData](index.html)(val firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val imageUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val hasImage: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val userId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null)

Public information about a user that can be shared with other users.



## Constructors


| | |
|---|---|
| [PublicUserData](-public-user-data.html) | [androidJvm]<br>constructor(firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, imageUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, hasImage: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), userId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null) |


## Properties


| Name | Summary |
|---|---|
| [firstName](first-name.html) | [androidJvm]<br>@SerialName(value = &quot;first_name&quot;)<br>val [firstName](first-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The user's first name. |
| [hasImage](has-image.html) | [androidJvm]<br>@SerialName(value = &quot;has_image&quot;)<br>val [hasImage](has-image.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>A boolean indicating whether the user has a profile image. |
| [identifier](identifier.html) | [androidJvm]<br>val [identifier](identifier.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The user's identifier (e.g., email address or phone number). |
| [imageUrl](image-url.html) | [androidJvm]<br>@SerialName(value = &quot;image_url&quot;)<br>val [imageUrl](image-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The user's profile image URL. |
| [lastName](last-name.html) | [androidJvm]<br>@SerialName(value = &quot;last_name&quot;)<br>val [lastName](last-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The user's last name. |
| [userId](user-id.html) | [androidJvm]<br>@SerialName(value = &quot;user_id&quot;)<br>val [userId](user-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The unique identifier of the user. |

