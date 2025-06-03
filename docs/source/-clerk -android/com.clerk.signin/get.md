---
title: get
---
//[Clerk Android](../../index.html)/[com.clerk.signin](index.html)/[get](get.html)



# get



[androidJvm]\
suspend fun [SignIn](-sign-in/index.html).[get](get.html)(rotatingTokenNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](-sign-in/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;



Retrieves the current state of the SignIn object from the server.



This function can be used to refresh the SignIn object and get the latest status and verification information.



#### Return



A [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html) containing the refreshed SignIn object.



#### Parameters


androidJvm

| | |
|---|---|
| rotatingTokenNonce | Optional nonce for rotating token validation. |




