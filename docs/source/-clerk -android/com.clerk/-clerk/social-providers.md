---
title: socialProviders
---
//[Clerk Android](../../../index.html)/[com.clerk](../index.html)/[Clerk](index.html)/[socialProviders](social-providers.html)



# socialProviders



[androidJvm]\
val [socialProviders](social-providers.html): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [UserSettings.SocialConfig](../../com.clerk.model.environment/-user-settings/-social-config/index.html)&gt;



Map of available social authentication providers configured for this application.



Each entry contains the provider's strategy identifier (e.g., &quot;oauth_google&quot;, &quot;oauth_facebook&quot;) and its configuration details. Use these strategy identifiers when initiating OAuth sign-in flows.



#### Return



Map where keys are strategy identifiers and values contain provider configuration.



#### See also


| | |
|---|---|
| [SignIn.Companion.create](../../com.clerk.signin/-sign-in/-companion/create.html) | for usage with OAuth authentication. |



