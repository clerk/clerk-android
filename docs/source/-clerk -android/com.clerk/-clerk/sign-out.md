---
title: signOut
---
//[Clerk Android](../../../index.html)/[com.clerk](../index.html)/[Clerk](index.html)/[signOut](sign-out.html)



# signOut



[androidJvm]\
suspend fun [signOut](sign-out.html)(): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;



Signs out the currently authenticated user.



This operation removes the active session from both the server and local storage, clearing all cached user data and authentication state.



#### Return



A [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html) indicating success or failure of the sign-out operation.




