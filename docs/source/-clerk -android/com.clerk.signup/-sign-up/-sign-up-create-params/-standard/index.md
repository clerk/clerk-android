---
title: Standard
---
//[Clerk Android](../../../../../index.html)/[com.clerk.signup](../../../index.html)/[SignUp](../../index.html)/[SignUpCreateParams](../index.html)/[Standard](index.html)



# Standard





@Serializable



data class [Standard](index.html)(val emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val phoneNumber: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null) : [SignUp.SignUpCreateParams](../index.html)

Standard sign-up strategy, allowing the user to provide common details such as email, password, and personal information.



#### Parameters


androidJvm

| | |
|---|---|
| emailAddress | The user's email address (optional). |
| password | The user's password (optional). |
| firstName | The user's first name (optional). |
| lastName | The user's last name (optional). |
| username | The user's username (optional). |
| phoneNumber | The user's phone number (optional). |



## Constructors


| | |
|---|---|
| [Standard](-standard.html) | [androidJvm]<br>constructor(emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, phoneNumber: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null) |


## Properties


| Name | Summary |
|---|---|
| [emailAddress](email-address.html) | [androidJvm]<br>@SerialName(value = &quot;email_address&quot;)<br>val [emailAddress](email-address.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null |
| [firstName](first-name.html) | [androidJvm]<br>@SerialName(value = &quot;first_name&quot;)<br>val [firstName](first-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null |
| [lastName](last-name.html) | [androidJvm]<br>@SerialName(value = &quot;last_name&quot;)<br>val [lastName](last-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null |
| [password](password.html) | [androidJvm]<br>val [password](password.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null |
| [phoneNumber](phone-number.html) | [androidJvm]<br>@SerialName(value = &quot;phone_number&quot;)<br>val [phoneNumber](phone-number.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null |
| [username](username.html) | [androidJvm]<br>val [username](username.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null |

