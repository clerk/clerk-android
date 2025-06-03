---
title: create
---
//[Clerk Android](../../../../index.html)/[com.clerk.signin](../../index.html)/[SignIn](../index.html)/[Companion](index.html)/[create](create.html)



# create



[androidJvm]\
suspend fun [create](create.html)(params: [SignIn.SignInCreateParams.Strategy](../-sign-in-create-params/-strategy/index.html)): [ClerkApiResult](../../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](../index.html), [ClerkErrorResponse](../../../com.clerk.model.error/-clerk-error-response/index.html)&gt;



Starts the sign in process. The SignIn object holds the state of the current sign-in and provides helper methods to navigate and complete the sign-in process. It is used to manage the sign-in lifecycle, including the first and second factor verification, and the creation of a new session.



The following steps outline the sign-in process:



1. 
   Initiate the sign-in process by collecting the user's authentication information and     passing the appropriate parameters to the `create()` method.
2. 
   Prepare the first factor verification by calling `SignIn.prepareFirstFactor()`. Users     *must* complete a first factor verification. This can be something like providing a     password, an email link, a one-time code (OTP), a Web3 wallet address, or providing proof     of their identity through an external social account (SSO/OAuth).
3. 
   Attempt to complete the first factor verification by calling [SignIn.attemptFirstFactor](../../attempt-first-factor.html).
4. 
   Optionally, if you have enabled multi-factor for your application, you will need to     prepare the second factor verification by calling `SignIn.prepareSecondFactor()`.
5. 
   Attempt to complete the second factor verification by calling     [SignIn.attemptSecondFactor()](../index.html)
6. 
   If verification is successful, set the newly created session as the active session by     passing the `SignIn.createdSessionId` to the `setActive()` method on the `Clerk` object.




NOTE: If you are using the `SignIn.authenticateWithRedirect()` method, you do not need to call `SignIn.create()` first. The `SignIn.authenticateWithRedirect()` method will handle the creation of the SignIn object internally.



#### Parameters


androidJvm

| | |
|---|---|
| params | The strategy to authenticate with. |



#### See also


| | |
|---|---|
| [SignIn.SignInCreateParams](../-sign-in-create-params/index.html) | Example usage:<br>```kotlin SignIn.create(SignInCreateParams.Strategy.EmailAddress("user@example.com"))          .onSuccess { signIn -> // Do something with the signIn object }          .onFailure { error -> // Handle the error } ``` |




[androidJvm]\
suspend fun [create](create.html)(params: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;): [ClerkApiResult](../../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](../index.html), [ClerkErrorResponse](../../../com.clerk.model.error/-clerk-error-response/index.html)&gt;



Creates a new SignIn object with the provided parameters. This is the equivalent of calling `SignIn.create()` with JSON.



#### Return



A [ClerkApiResult](../../../com.clerk.network.serialization/-clerk-api-result/index.html) containing the created SignIn object.



Example usage:

```kotlin
val signIn = SignIn.create(mapOf("identifier" to "user@example.com"))
                      .onSuccess { signIn -> Do something with the signIn object }
                      .onFailure { error -> Handle the error }
```


#### Parameters


androidJvm

| | |
|---|---|
| params | The raw parameters to create the SignIn object with. |




