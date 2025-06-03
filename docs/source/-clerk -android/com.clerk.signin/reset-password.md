---
title: resetPassword
---
//[Clerk Android](../../index.html)/[com.clerk.signin](index.html)/[resetPassword](reset-password.html)



# resetPassword



[androidJvm]\
suspend fun [SignIn](-sign-in/index.html).[resetPassword](reset-password.html)(params: [SignIn.ResetPasswordParams](-sign-in/-reset-password-params/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](-sign-in/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;



Resets the password for the current sign in attempt.



This function is used when a user needs to reset their password during the sign-in process, typically after receiving a password reset verification code.



#### Return



A [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html) containing the updated SignIn object after the password reset.



#### Parameters


androidJvm

| | |
|---|---|
| params | An instance of [SignIn.ResetPasswordParams](-sign-in/-reset-password-params/index.html) containing the new password and session options. |



#### See also


| |
|---|
| [SignIn.ResetPasswordParams](-sign-in/-reset-password-params/index.html) |




[androidJvm]\
suspend fun [SignIn](-sign-in/index.html).[resetPassword](reset-password.html)(password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), signOutOfOtherSessions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](-sign-in/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;



Resets the password for the current sign in attempt.



This function is used when a user needs to reset their password during the sign-in process, typically after receiving a password reset verification code.



#### Return



A [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html) containing the updated SignIn object after the password reset.



#### Parameters


androidJvm

| | |
|---|---|
| password | An instance of [SignIn.ResetPasswordParams](-sign-in/-reset-password-params/index.html) containing the new password and session options. |
| signOutOfOtherSessions | Whether to sign out of other sessions after resetting the password. Defaults to false. |



#### See also


| |
|---|
| [SignIn.ResetPasswordParams](-sign-in/-reset-password-params/index.html) |



