---
title: attemptFirstFactor
---
//[Clerk Android](../../index.html)/[com.clerk.signin](index.html)/[attemptFirstFactor](attempt-first-factor.html)



# attemptFirstFactor



[androidJvm]\
suspend fun [SignIn](-sign-in/index.html).[attemptFirstFactor](attempt-first-factor.html)(params: [SignIn.AttemptFirstFactorParams](-sign-in/-attempt-first-factor-params/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](-sign-in/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;



Attempts to complete the first factor verification process. This is a required step in order to complete a sign in, as users should be verified at least by one factor of authentication.



Make sure that a SignIn object already exists before you call this method, either by first calling SignIn.create() or SignIn.prepareFirstFactor(). The only strategy that does not require a verification to have already been prepared before attempting to complete it is the password strategy.



Depending on the strategy that was selected when the verification was prepared, the method parameters will be different.



Returns a SignIn object. Check the firstFactorVerification attribute for the status of the first factor verification process.



#### Return



A [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html) containing the updated SignIn object with the first factor verification result.



#### Parameters


androidJvm

| | |
|---|---|
| params | The parameters for the first factor verification. |



#### See also


| |
|---|
| [SignIn.AttemptFirstFactorParams](-sign-in/-attempt-first-factor-params/index.html) |



