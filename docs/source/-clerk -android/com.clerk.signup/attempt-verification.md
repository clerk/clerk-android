---
title: attemptVerification
---
//[Clerk Android](../../index.html)/[com.clerk.signup](index.html)/[attemptVerification](attempt-verification.html)



# attemptVerification



[androidJvm]\
suspend fun [SignUp](-sign-up/index.html).[attemptVerification](attempt-verification.html)(params: [SignUp.AttemptVerificationParams](-sign-up/-attempt-verification-params/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignUp](-sign-up/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;



// Attempts to complete the in-flight verification process that corresponds to the given strategy. In order to use this method, you should first initiate a verification process by calling SignUp.prepareVerification.



Depending on the strategy, the method parameters could differ.



#### Parameters


androidJvm

| | |
|---|---|
| params | : The parameters for the verification attempt. This includes the strategy and the code @return: The updated [SignUp](-sign-up/index.html) object reflecting the verification attempt's result. |




