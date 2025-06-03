---
title: SignUpCreateParams
---
//[Clerk Android](../../../../index.html)/[com.clerk.signup](../../index.html)/[SignUp](../index.html)/[SignUpCreateParams](index.html)



# SignUpCreateParams

sealed interface [SignUpCreateParams](index.html)

Represents the various strategies for initiating a `SignUp` request. This sealed interface encapsulates the different ways to create a sign-up, such as using standard parameters (e.g., email, password) or creating without any parameters to inspect the signUp object first.



#### Inheritors


| |
|---|
| [Standard](-standard/index.html) |
| [None](-none/index.html) |
| [Transfer](-transfer/index.html) |


## Types


| Name | Summary |
|---|---|
| [None](-none/index.html) | [androidJvm]<br>object [None](-none/index.html) : [SignUp.SignUpCreateParams](index.html)<br>The `SignUp` will be created without any parameters. |
| [Standard](-standard/index.html) | [androidJvm]<br>@Serializable<br>data class [Standard](-standard/index.html)(val emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val phoneNumber: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null) : [SignUp.SignUpCreateParams](index.html)<br>Standard sign-up strategy, allowing the user to provide common details such as email, password, and personal information. |
| [Transfer](-transfer/index.html) | [androidJvm]<br>object [Transfer](-transfer/index.html) : [SignUp.SignUpCreateParams](index.html) |

