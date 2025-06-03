---
title: fromStrategy
---
//[Clerk Android](../../../../index.html)/[com.clerk.sso](../../index.html)/[OAuthProvider](../index.html)/[Companion](index.html)/[fromStrategy](from-strategy.html)



# fromStrategy



[androidJvm]\
fun [fromStrategy](from-strategy.html)(strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [OAuthProvider](../index.html)



Convenience function to retrieve an OAuthProvider from its strategy string. Generally used to take a [com.clerk.model.environment.UserSettings.SocialConfig.strategy](../../../com.clerk.model.environment/-user-settings/-social-config/strategy.html) and convert it to an OAuthProvider.



#### Return



the corresponding OAuthProvider.



#### Parameters


androidJvm

| | |
|---|---|
| strategy | the strategy string to match against. |



#### Throws


| | |
|---|---|
| [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) | if no matching provider is found. |



