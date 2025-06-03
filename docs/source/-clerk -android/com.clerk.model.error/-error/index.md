---
title: Error
---
//[Clerk Android](../../../index.html)/[com.clerk.model.error](../index.html)/[Error](index.html)



# Error



[androidJvm]\
@Serializable



data class [Error](index.html)(val message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val longMessage: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))



## Constructors


| | |
|---|---|
| [Error](-error.html) | [androidJvm]<br>constructor(message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), longMessage: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), code: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [code](code.html) | [androidJvm]<br>val [code](code.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>A string code that represents the error, such as `username_exists_code`. |
| [longMessage](long-message.html) | [androidJvm]<br>@SerialName(value = &quot;long_message&quot;)<br>val [longMessage](long-message.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>A more detailed message that describes the error. |
| [message](message.html) | [androidJvm]<br>val [message](message.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>A message that describes the error. |

