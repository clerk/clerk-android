---
title: Companion
---
//[Clerk Android](../../../../index.html)/[com.clerk.signin](../../index.html)/[SignIn](../index.html)/[Companion](index.html)



# Companion



[androidJvm]\
object [Companion](index.html)



## Functions


| Name | Summary |
|---|---|
| [authenticateWithRedirect](authenticate-with-redirect.html) | [androidJvm]<br>suspend fun [authenticateWithRedirect](authenticate-with-redirect.html)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), params: [SignIn.AuthenticateWithRedirectParams](../-authenticate-with-redirect-params/index.html)): [ClerkApiResult](../../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SSOResult](../../../com.clerk.sso/-s-s-o-result/index.html), [ClerkErrorResponse](../../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Initiates the sign-in process using an OAuth or Enterprise SSO redirect flow. |
| [create](create.html) | [androidJvm]<br>suspend fun [create](create.html)(params: [SignIn.SignInCreateParams.Strategy](../-sign-in-create-params/-strategy/index.html)): [ClerkApiResult](../../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](../index.html), [ClerkErrorResponse](../../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Starts the sign in process. The SignIn object holds the state of the current sign-in and provides helper methods to navigate and complete the sign-in process. It is used to manage the sign-in lifecycle, including the first and second factor verification, and the creation of a new session.<br>[androidJvm]<br>suspend fun [create](create.html)(params: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;): [ClerkApiResult](../../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](../index.html), [ClerkErrorResponse](../../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Creates a new SignIn object with the provided parameters. This is the equivalent of calling `SignIn.create()` with JSON. |

