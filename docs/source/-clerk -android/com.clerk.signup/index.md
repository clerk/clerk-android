---
title: com.clerk.signup
---
//[Clerk Android](../../index.html)/[com.clerk.signup](index.html)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [SignUp](-sign-up/index.html) | [androidJvm]<br>@Serializable<br>data class [SignUp](-sign-up/index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val status: [SignUp.Status](-sign-up/-status/index.html), val requiredFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val optionalFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val missingFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val unverifiedFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val verifications: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [Verification](../com.clerk.model.verification/-verification/index.html)?&gt;, val username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val phoneNumber: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val passwordEnabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val unsafeMetadata: JsonObject? = null, val createdSessionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val createdUserId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val abandonedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null)<br>The `SignUp` object holds the state of the current sign-up and provides helper methods to navigate and complete the sign-up process. Once a sign-up is complete, a new user is created. |
| [SignUpUpdateParams](-sign-up-update-params/index.html) | [androidJvm]<br>typealias [SignUpUpdateParams](-sign-up-update-params/index.html) = [SignUp.SignUpCreateParams](-sign-up/-sign-up-create-params/index.html) |


## Functions


| Name | Summary |
|---|---|
| [attemptVerification](attempt-verification.html) | [androidJvm]<br>suspend fun [SignUp](-sign-up/index.html).[attemptVerification](attempt-verification.html)(params: [SignUp.AttemptVerificationParams](-sign-up/-attempt-verification-params/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignUp](-sign-up/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>// Attempts to complete the in-flight verification process that corresponds to the given strategy. In order to use this method, you should first initiate a verification process by calling SignUp.prepareVerification. |
| [prepareVerification](prepare-verification.html) | [androidJvm]<br>suspend fun [SignUp](-sign-up/index.html).[prepareVerification](prepare-verification.html)(prepareVerification: [SignUp.PrepareVerificationParams.Strategy](-sign-up/-prepare-verification-params/-strategy/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignUp](-sign-up/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>The [prepareVerification](prepare-verification.html) method is used to initiate the verification process for a field that requires it. |
| [toSSOResult](to-s-s-o-result.html) | [androidJvm]<br>fun [SignUp](-sign-up/index.html).[toSSOResult](to-s-s-o-result.html)(): [SSOResult](../com.clerk.sso/-s-s-o-result/index.html)<br>Converts the [SignUp](-sign-up/index.html) object to an [SSOResult](../com.clerk.sso/-s-s-o-result/index.html) object. |
| [update](update.html) | [androidJvm]<br>suspend fun [SignUp](-sign-up/index.html).[update](update.html)(updateParams: [SignUpUpdateParams](-sign-up-update-params/index.html)): [ClerkApiResult](../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignUp](-sign-up/index.html), [ClerkErrorResponse](../com.clerk.model.error/-clerk-error-response/index.html)&gt; |

