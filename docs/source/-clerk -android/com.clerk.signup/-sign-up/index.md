---
title: SignUp
---
//[Clerk Android](../../../index.html)/[com.clerk.signup](../index.html)/[SignUp](index.html)



# SignUp



[androidJvm]\
@Serializable



data class [SignUp](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val status: [SignUp.Status](-status/index.html), val requiredFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val optionalFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val missingFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val unverifiedFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, val verifications: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [Verification](../../com.clerk.model.verification/-verification/index.html)?&gt;, val username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val phoneNumber: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val passwordEnabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val unsafeMetadata: JsonObject? = null, val createdSessionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val createdUserId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val abandonedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null)

The `SignUp` object holds the state of the current sign-up and provides helper methods to navigate and complete the sign-up process. Once a sign-up is complete, a new user is created.



### The Sign-Up Process:



1. 
   **Initiate the Sign-Up**: Begin the sign-up process by collecting the user's authentication     information and passing the appropriate parameters to the `create()` method.
2. 
   **Prepare the Verification**: The system will prepare the necessary verification steps to     confirm the user's information.
3. 
   **Complete the Verification**: Attempt to complete the verification by following the required     steps based on the collected authentication data.
4. 
   **Sign Up Complete**: If the verification is successful, the newly created session is set as     the active session.



## Constructors


| | |
|---|---|
| [SignUp](-sign-up.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), status: [SignUp.Status](-status/index.html), requiredFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, optionalFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, missingFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, unverifiedFields: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;, verifications: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [Verification](../../com.clerk.model.verification/-verification/index.html)?&gt;, username: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, emailAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, phoneNumber: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, passwordEnabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, unsafeMetadata: JsonObject? = null, createdSessionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, createdUserId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, abandonedAt: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null) |


## Types


| Name | Summary |
|---|---|
| [AttemptVerificationParams](-attempt-verification-params/index.html) | [androidJvm]<br>sealed interface [AttemptVerificationParams](-attempt-verification-params/index.html)<br>Defines the possible strategies for attempting verification during the sign-up process. This sealed interface encapsulates the different types of verification attempts, such as email or phone code verification. |
| [Companion](-companion/index.html) | [androidJvm]<br>object [Companion](-companion/index.html) |
| [PrepareVerificationParams](-prepare-verification-params/index.html) | [androidJvm]<br>object [PrepareVerificationParams](-prepare-verification-params/index.html) |
| [SignUpCreateParams](-sign-up-create-params/index.html) | [androidJvm]<br>sealed interface [SignUpCreateParams](-sign-up-create-params/index.html)<br>Represents the various strategies for initiating a `SignUp` request. This sealed interface encapsulates the different ways to create a sign-up, such as using standard parameters (e.g., email, password) or creating without any parameters to inspect the signUp object first. |
| [Status](-status/index.html) | [androidJvm]<br>@Serializable<br>enum [Status](-status/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[SignUp.Status](-status/index.html)&gt; <br>Represents the current status of the sign-up process. |


## Properties


| Name | Summary |
|---|---|
| [abandonedAt](abandoned-at.html) | [androidJvm]<br>@SerialName(value = &quot;abandoned_at&quot;)<br>val [abandonedAt](abandoned-at.html): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)? = null<br>The date when the sign-up was abandoned by the user. |
| [createdSessionId](created-session-id.html) | [androidJvm]<br>@SerialName(value = &quot;created_session_id&quot;)<br>val [createdSessionId](created-session-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The identifier of the newly-created session. This attribute is populated only when the sign-up is complete. |
| [createdUserId](created-user-id.html) | [androidJvm]<br>@SerialName(value = &quot;created_user_id&quot;)<br>val [createdUserId](created-user-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The identifier of the newly-created user. This attribute is populated only when the sign-up is complete. |
| [emailAddress](email-address.html) | [androidJvm]<br>@SerialName(value = &quot;email_address&quot;)<br>val [emailAddress](email-address.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The email address supplied to the current sign-up. Only supported if email address is enabled in the instance settings. |
| [firstName](first-name.html) | [androidJvm]<br>@SerialName(value = &quot;first_name&quot;)<br>val [firstName](first-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The first name supplied to the current sign-up. Only supported if name is enabled in the instance settings. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The unique identifier of the current sign-up. |
| [lastName](last-name.html) | [androidJvm]<br>@SerialName(value = &quot;last_name&quot;)<br>val [lastName](last-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The last name supplied to the current sign-up. Only supported if name is enabled in the instance settings. |
| [missingFields](missing-fields.html) | [androidJvm]<br>@SerialName(value = &quot;missing_fields&quot;)<br>val [missingFields](missing-fields.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>An array of all the fields whose values are not supplied yet but they are mandatory in order for a sign-up to be marked as complete. |
| [optionalFields](optional-fields.html) | [androidJvm]<br>@SerialName(value = &quot;optional_fields&quot;)<br>val [optionalFields](optional-fields.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>An array of all the fields that can be supplied to the sign-up, but their absence does not prevent the sign-up from being marked as complete. |
| [passwordEnabled](password-enabled.html) | [androidJvm]<br>@SerialName(value = &quot;password_enabled&quot;)<br>val [passwordEnabled](password-enabled.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>The value of this attribute is true if a password was supplied to the current sign-up. Only supported if password is enabled in the instance settings. |
| [phoneNumber](phone-number.html) | [androidJvm]<br>@SerialName(value = &quot;phone_number&quot;)<br>val [phoneNumber](phone-number.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The user's phone number in E.164 format. Only supported if phone number is enabled in the instance settings. |
| [requiredFields](required-fields.html) | [androidJvm]<br>@SerialName(value = &quot;required_fields&quot;)<br>val [requiredFields](required-fields.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>An array of all the required fields that need to be supplied and verified in order for this sign-up to be marked as complete and converted into a user. |
| [status](status.html) | [androidJvm]<br>val [status](status.html): [SignUp.Status](-status/index.html)<br>The status of the current sign-up. |
| [unsafeMetadata](unsafe-metadata.html) | [androidJvm]<br>@SerialName(value = &quot;unsafe_metadata&quot;)<br>val [unsafeMetadata](unsafe-metadata.html): JsonObject? = null<br>Metadata that can be read and set from the frontend. Once the sign-up is complete, the value of this field will be automatically copied to the newly created user's unsafe metadata. One common use case for this attribute is to use it to implement custom fields that can be collected during sign-up and will automatically be attached to the created User object. |
| [unverifiedFields](unverified-fields.html) | [androidJvm]<br>@SerialName(value = &quot;unverified_fields&quot;)<br>val [unverifiedFields](unverified-fields.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;<br>An array of all the fields whose values have been supplied, but they need additional verification in order for them to be accepted. |
| [username](username.html) | [androidJvm]<br>val [username](username.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The username supplied to the current sign-up. Only supported if username is enabled in the instance settings. |
| [verifications](verifications.html) | [androidJvm]<br>val [verifications](verifications.html): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [Verification](../../com.clerk.model.verification/-verification/index.html)?&gt;<br>An object that contains information about all the verifications that are in-flight. |


## Functions


| Name | Summary |
|---|---|
| [attemptVerification](../attempt-verification.html) | [androidJvm]<br>suspend fun [SignUp](index.html).[attemptVerification](../attempt-verification.html)(params: [SignUp.AttemptVerificationParams](-attempt-verification-params/index.html)): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignUp](index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>// Attempts to complete the in-flight verification process that corresponds to the given strategy. In order to use this method, you should first initiate a verification process by calling SignUp.prepareVerification. |
| [prepareVerification](../prepare-verification.html) | [androidJvm]<br>suspend fun [SignUp](index.html).[prepareVerification](../prepare-verification.html)(prepareVerification: [SignUp.PrepareVerificationParams.Strategy](-prepare-verification-params/-strategy/index.html)): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignUp](index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>The [prepareVerification](../prepare-verification.html) method is used to initiate the verification process for a field that requires it. |
| [toSSOResult](../to-s-s-o-result.html) | [androidJvm]<br>fun [SignUp](index.html).[toSSOResult](../to-s-s-o-result.html)(): [SSOResult](../../com.clerk.sso/-s-s-o-result/index.html)<br>Converts the [SignUp](index.html) object to an [SSOResult](../../com.clerk.sso/-s-s-o-result/index.html) object. |
| [update](../update.html) | [androidJvm]<br>suspend fun [SignUp](index.html).[update](../update.html)(updateParams: [SignUpUpdateParams](../-sign-up-update-params/index.html)): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignUp](index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt; |

