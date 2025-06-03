---
title: Clerk
---
//[Clerk Android](../../../index.html)/[com.clerk](../index.html)/[Clerk](index.html)



# Clerk



[androidJvm]\
object [Clerk](index.html)

Main entrypoint class for the Clerk SDK.



Provides access to authentication state, user information, and core functionality for managing user sessions and sign-in flows.



## Properties


| Name | Summary |
|---|---|
| [client](client.html) | [androidJvm]<br>lateinit var [client](client.html): [Client](../../com.clerk.model.client/-client/index.html)<br>The Client object representing the current device and its authentication state. |
| [debugMode](debug-mode.html) | [androidJvm]<br>var [debugMode](debug-mode.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Enable for additional debugging signals and logging. |
| [isInitialized](is-initialized.html) | [androidJvm]<br>val [isInitialized](is-initialized.html): StateFlow&lt;[Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)&gt;<br>Reactive state indicating whether the Clerk SDK has completed initialization. |
| [isSignedIn](is-signed-in.html) | [androidJvm]<br>val [isSignedIn](is-signed-in.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Indicates whether a user is currently signed in. |
| [logoUrl](logo-url.html) | [androidJvm]<br>val [logoUrl](logo-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The image URL for the application logo used in authentication UI components. |
| [session](session.html) | [androidJvm]<br>val [session](session.html): [Session](../../com.clerk.model.session/-session/index.html)?<br>The currently active user session. |
| [signIn](sign-in.html) | [androidJvm]<br>val [signIn](sign-in.html): [SignIn](../../com.clerk.signin/-sign-in/index.html)?<br>The current sign-in attempt, if one is in progress. |
| [socialProviders](social-providers.html) | [androidJvm]<br>val [socialProviders](social-providers.html): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [UserSettings.SocialConfig](../../com.clerk.model.environment/-user-settings/-social-config/index.html)&gt;<br>Map of available social authentication providers configured for this application. |
| [user](user.html) | [androidJvm]<br>val [user](user.html): [User](../../com.clerk.model.user/-user/index.html)?<br>The currently authenticated user. |


## Functions


| Name | Summary |
|---|---|
| [initialize](initialize.html) | [androidJvm]<br>fun [initialize](initialize.html)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), publishableKey: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), debugMode: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false)<br>Initializes the Clerk SDK with the provided configuration. |
| [signOut](sign-out.html) | [androidJvm]<br>suspend fun [signOut](sign-out.html)(): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Signs out the currently authenticated user. |

