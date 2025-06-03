---
title: secondFactorVerification
---
//[Clerk Android](../../../index.html)/[com.clerk.signin](../index.html)/[SignIn](index.html)/[secondFactorVerification](second-factor-verification.html)



# secondFactorVerification



[androidJvm]\




@SerialName(value = &quot;second_factor_verification&quot;)



val [secondFactorVerification](second-factor-verification.html): [Verification](../../com.clerk.model.verification/-verification/index.html)? = null



The state of the verification process for the selected second factor.



Initially, this property contains an empty verification object, since there is no second factor selected. For the `phone_code` strategy, you need to call the `prepareSecondFactor` method in order to start the verification process. For the `totp` strategy, you can directly attempt.




