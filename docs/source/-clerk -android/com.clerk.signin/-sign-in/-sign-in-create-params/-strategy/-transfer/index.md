---
title: Transfer
---
//[Clerk Android](../../../../../../index.html)/[com.clerk.signin](../../../../index.html)/[SignIn](../../../index.html)/[SignInCreateParams](../../index.html)/[Strategy](../index.html)/[Transfer](index.html)



# Transfer



[androidJvm]\
data class [Transfer](index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = TRANSFER) : [SignIn.SignInCreateParams.Strategy](../index.html)

Transfer strategy for account transfer scenarios.



This strategy is used when transferring an existing session or account state.



## Constructors


| | |
|---|---|
| [Transfer](-transfer.html) | [androidJvm]<br>constructor()constructor(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = TRANSFER) |


## Properties


| Name | Summary |
|---|---|
| [strategy](strategy.html) | [androidJvm]<br>open override val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The authentication strategy identifier. |

