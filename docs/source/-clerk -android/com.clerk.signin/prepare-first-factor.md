---
title: prepareFirstFactor
---
//[Clerk Android](../../index.html)/[com.clerk.signin](index.html)/[prepareFirstFactor](prepare-first-factor.html)



# prepareFirstFactor



[androidJvm]\
suspend fun [SignIn](-sign-in/index.html).[prepareFirstFactor](prepare-first-factor.html)(strategy: [SignIn.PrepareFirstFactorParams.Strategy](-sign-in/-prepare-first-factor-params/-strategy/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](-sign-in/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;



Begins the first factor verification process. This is a required step in order to complete a sign in, as users should be verified at least by one factor of authentication.



Common scenarios are one-time code (OTP) or social account (SSO) verification. This is determined by the accepted strategy parameter values. Each authentication identifier supports different strategies.



Returns a SignIn object. Check the firstFactorVerification attribute for the status of the first factor verification process.



#### Return



A [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html) containing the updated SignIn object with the prepared first factor verification.



#### Parameters


androidJvm

| | |
|---|---|
| strategy | The strategy to authenticate with. |



#### See also


| | |
|---|---|
| [SignIn.PrepareFirstFactorParams](-sign-in/-prepare-first-factor-params/index.html) | Example usage:<br>```kotlin Clerk.signIn.prepareFirstFactor(strategy = PrepareFirstFactorParams.Strategy.EmailCode)   .onSuccess { updatedSignIn ->  // Handle the updated SignIn object }   .onFailure { error ->  // Handle the error } ``` |



