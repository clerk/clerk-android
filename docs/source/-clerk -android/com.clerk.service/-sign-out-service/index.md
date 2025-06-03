---
title: SignOutService
---
//[Clerk Android](../../../index.html)/[com.clerk.service](../index.html)/[SignOutService](index.html)



# SignOutService



[androidJvm]\
object [SignOutService](index.html)

Service responsible for signing out users by removing their active session.



The SignOutService handles the complete sign-out process, including removing the session from the Clerk API and cleaning up local session state. It performs network operations asynchronously and provides proper error handling.



## Functions


| Name | Summary |
|---|---|
| [signOut](sign-out.html) | [androidJvm]<br>suspend fun [signOut](sign-out.html)(): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Signs out the currently authenticated user by removing their active session. |

