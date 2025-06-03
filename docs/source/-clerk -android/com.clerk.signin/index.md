---
title: com.clerk.signin
---
//[Clerk Android](../../index.html)/[com.clerk.signin](index.html)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [SignIn](-sign-in/index.html) | [androidJvm]<br>@Serializable<br>data class [SignIn](-sign-in/index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val status: [SignIn.Status](-sign-in/-status/index.html), val supportedIdentifiers: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null, val identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val supportedFirstFactors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Factor](../com.clerk.model.factor/-factor/index.html)&gt;? = null, val supportedSecondFactors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Factor](../com.clerk.model.factor/-factor/index.html)&gt;? = null, val firstFactorVerification: [Verification](../com.clerk.model.verification/-verification/index.html)? = null, val secondFactorVerification: [Verification](../com.clerk.model.verification/-verification/index.html)? = null, val userData: [SignIn.UserData](-sign-in/-user-data/index.html)? = null, val createdSessionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null)<br>The `SignIn` object holds the state of the current sign-in process and provides helper methods to navigate and complete the sign-in lifecycle. This includes managing the first and second factor verifications, as well as creating a new session. |


## Functions


| Name | Summary |
|---|---|
| [attemptFirstFactor](attempt-first-factor.html) | [androidJvm]<br>suspend fun [SignIn](-sign-in/index.html).[attemptFirstFactor](attempt-first-factor.html)(params: [SignIn.AttemptFirstFactorParams](-sign-in/-attempt-first-factor-params/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](-sign-in/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Attempts to complete the first factor verification process. This is a required step in order to complete a sign in, as users should be verified at least by one factor of authentication. |
| [get](get.html) | [androidJvm]<br>suspend fun [SignIn](-sign-in/index.html).[get](get.html)(rotatingTokenNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](-sign-in/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Retrieves the current state of the SignIn object from the server. |
| [prepareFirstFactor](prepare-first-factor.html) | [androidJvm]<br>suspend fun [SignIn](-sign-in/index.html).[prepareFirstFactor](prepare-first-factor.html)(strategy: [SignIn.PrepareFirstFactorParams.Strategy](-sign-in/-prepare-first-factor-params/-strategy/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](-sign-in/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Begins the first factor verification process. This is a required step in order to complete a sign in, as users should be verified at least by one factor of authentication. |
| [resetPassword](reset-password.html) | [androidJvm]<br>suspend fun [SignIn](-sign-in/index.html).[resetPassword](reset-password.html)(params: [SignIn.ResetPasswordParams](-sign-in/-reset-password-params/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](-sign-in/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>suspend fun [SignIn](-sign-in/index.html).[resetPassword](reset-password.html)(password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), signOutOfOtherSessions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](-sign-in/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Resets the password for the current sign in attempt. |
| [toSSOResult](to-s-s-o-result.html) | [androidJvm]<br>fun [SignIn](-sign-in/index.html).[toSSOResult](to-s-s-o-result.html)(): [SSOResult](../com.clerk.sso/-s-s-o-result/index.html)<br>Converts the current [SignIn](-sign-in/index.html) instance to an [SSOResult](../com.clerk.sso/-s-s-o-result/index.html). |

