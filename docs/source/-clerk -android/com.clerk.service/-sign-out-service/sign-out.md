---
title: signOut
---
//[Clerk Android](../../../index.html)/[com.clerk.service](../index.html)/[SignOutService](index.html)/[signOut](sign-out.html)



# signOut



[androidJvm]\
suspend fun [signOut](sign-out.html)(): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;



Signs out the currently authenticated user by removing their active session.



This method will attempt to remove the session from the Clerk API if a session ID exists, otherwise it will delete the local session. The operation is performed asynchronously and includes proper error handling.



#### Return



A [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html) indicating the success or failure of the sign-out operation. Returns [ClerkApiResult.success](../../com.clerk.network.serialization/-clerk-api-result/-companion/success.html) with [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html) on successful sign-out, or [ClerkApiResult.unknownFailure](../../com.clerk.network.serialization/-clerk-api-result/-companion/unknown-failure.html) with error details on failure.




