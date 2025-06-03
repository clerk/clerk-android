---
title: Factor
---
//[Clerk Android](../../../index.html)/[com.clerk.model.factor](../index.html)/[Factor](index.html)



# Factor



[androidJvm]\
@Serializable



data class [Factor](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val emailAddressId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val phoneNumberId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val web3WalletId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val safeIdentifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val primary: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null)

The Factor type represents the factor verification strategy that can be used in the sign-in process.



## Constructors


| | |
|---|---|
| [Factor](-factor.html) | [androidJvm]<br>constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), emailAddressId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, phoneNumberId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, web3WalletId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, safeIdentifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, primary: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null) |


## Properties


| Name | Summary |
|---|---|
| [emailAddressId](email-address-id.html) | [androidJvm]<br>@SerialName(value = &quot;email_address_id&quot;)<br>val [emailAddressId](email-address-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The ID of the email address that a code or link will be sent to. |
| [phoneNumberId](phone-number-id.html) | [androidJvm]<br>@SerialName(value = &quot;phone_number_id&quot;)<br>val [phoneNumberId](phone-number-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The ID of the phone number that a code will be sent to. |
| [primary](primary.html) | [androidJvm]<br>val [primary](primary.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null<br>Whether the factor is the primary factor. |
| [safeIdentifier](safe-identifier.html) | [androidJvm]<br>@SerialName(value = &quot;safe_identifier&quot;)<br>val [safeIdentifier](safe-identifier.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The safe identifier of the factor. |
| [strategy](strategy.html) | [androidJvm]<br>val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The strategy of the factor. |
| [web3WalletId](web3-wallet-id.html) | [androidJvm]<br>val [web3WalletId](web3-wallet-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The ID of the Web3 wallet that will be used to sign a message. |

