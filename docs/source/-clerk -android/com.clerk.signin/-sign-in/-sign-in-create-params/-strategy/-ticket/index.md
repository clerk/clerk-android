---
title: Ticket
---
//[Clerk Android](../../../../../../index.html)/[com.clerk.signin](../../../../index.html)/[SignIn](../../../index.html)/[SignInCreateParams](../../index.html)/[Strategy](../index.html)/[Ticket](index.html)



# Ticket





@Serializable



data class [Ticket](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = TICKET, val ticket: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) : [SignIn.SignInCreateParams.Strategy](../index.html)

Ticket strategy for authentication using a ticket.



#### Parameters


androidJvm

| | |
|---|---|
| strategy | The strategy identifier for ticket authentication. |
| identifier | The identifier for the ticket authentication. |
| ticket | The ticket used for authentication. ** |



## Constructors


| | |
|---|---|
| [Ticket](-ticket.html) | [androidJvm]<br>constructor(ticket: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = TICKET, ticket: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)) |


## Properties


| Name | Summary |
|---|---|
| [strategy](strategy.html) | [androidJvm]<br>open override val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [ticket](ticket.html) | [androidJvm]<br>val [ticket](ticket.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |

