---
title: DeletedObject
---
//[Clerk Android](../../../index.html)/[com.clerk.model.deleted](../index.html)/[DeletedObject](index.html)



# DeletedObject



[androidJvm]\
@Serializable



data class [DeletedObject](index.html)(val objectType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val deleted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null)

The DeletedObject class represents an item that has been deleted from the database.



## Constructors


| | |
|---|---|
| [DeletedObject](-deleted-object.html) | [androidJvm]<br>constructor(objectType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, deleted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null) |


## Properties


| Name | Summary |
|---|---|
| [deleted](deleted.html) | [androidJvm]<br>val [deleted](deleted.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null<br>A boolean checking if the item has been deleted or not. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The ID of the deleted item. |
| [objectType](object-type.html) | [androidJvm]<br>@SerialName(value = &quot;object&quot;)<br>val [objectType](object-type.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The object type that has been deleted. |

