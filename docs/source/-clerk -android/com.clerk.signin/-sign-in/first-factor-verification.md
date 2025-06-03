---
title: firstFactorVerification
---
//[Clerk Android](../../../index.html)/[com.clerk.signin](../index.html)/[SignIn](index.html)/[firstFactorVerification](first-factor-verification.html)



# firstFactorVerification



[androidJvm]\




@SerialName(value = &quot;first_factor_verification&quot;)



val [firstFactorVerification](first-factor-verification.html): [Verification](../../com.clerk.model.verification/-verification/index.html)? = null



The state of the verification process for the selected first factor.



Initially, this property contains an empty verification object, since there is no first factor selected. You need to call the `prepareFirstFactor` method in order to start the verification process.




