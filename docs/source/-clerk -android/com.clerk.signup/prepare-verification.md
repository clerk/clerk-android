---
title: prepareVerification
---
//[Clerk Android](../../index.html)/[com.clerk.signup](index.html)/[prepareVerification](prepare-verification.html)



# prepareVerification



[androidJvm]\
suspend fun [SignUp](-sign-up/index.html).[prepareVerification](prepare-verification.html)(prepareVerification: [SignUp.PrepareVerificationParams.Strategy](-sign-up/-prepare-verification-params/-strategy/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignUp](-sign-up/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;



The [prepareVerification](prepare-verification.html) method is used to initiate the verification process for a field that requires it.



There are two fields that need to be verified: [SignUp.emailAddress](-sign-up/email-address.html): The email address can be verified via an email code. This is a one-time code that is sent to the email already provided to the [SignUp](-sign-up/index.html) object. The [prepareVerification](prepare-verification.html) sends this email. [SignUp.phoneNumber](-sign-up/phone-number.html): The phone number can be verified via a phone code. This is a one-time code that is sent via an SMS to the phone already provided to the [SignUp](-sign-up/index.html) object. The [prepareVerification](prepare-verification.html) sends this SMS.



#### Return



A [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html) containing the result of the verification preparation. A successful response indicates that the verification process has been initiated, and the [SignUp](-sign-up/index.html) object is returned.



#### Parameters


androidJvm

| | |
|---|---|
| prepareVerification | : The parameters for preparing the verification.Specifies the field which requires verification |




