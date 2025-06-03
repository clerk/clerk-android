---
title: SignIn
---
//[Clerk Android](../../../index.html)/[com.clerk.signin](../index.html)/[SignIn](index.html)



# SignIn



[androidJvm]\
@Serializable



data class [SignIn](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val status: [SignIn.Status](-status/index.html), val supportedIdentifiers: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null, val identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val supportedFirstFactors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Factor](../../com.clerk.model.factor/-factor/index.html)&gt;? = null, val supportedSecondFactors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Factor](../../com.clerk.model.factor/-factor/index.html)&gt;? = null, val firstFactorVerification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, val secondFactorVerification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, val userData: [SignIn.UserData](-user-data/index.html)? = null, val createdSessionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null)

The `SignIn` object holds the state of the current sign-in process and provides helper methods to navigate and complete the sign-in lifecycle. This includes managing the first and second factor verifications, as well as creating a new session.



### The following steps outline the sign-in process:



1. 
   **Initiate the Sign-In Process**
   
   
   
    Collect the user's authentication information and pass the appropriate parameters to the     `SignIn.create()` method to start the sign-in.
2. 
   **Prepare for First Factor Verification**
   
   
   
    Users **must** complete a first factor verification. This can include:
3. - 
      Providing a password
   - 
      Using an email link
   - 
      Entering a one-time code (OTP)
   - 
      Authenticating with a Web3 wallet address
   - 
      Providing proof of identity through an external social account (SSO/OAuth).
4. 
   **Complete First Factor Verification**
   
   
   
    Attempt to verify the user's first factor authentication details.
5. 
   **Prepare for Second Factor Verification (Optional)**
   
   
   
    If multi-factor authentication (MFA) is enabled for your application, prepare the second     factor verification for users who have set up 2FA for their account.
6. 
   **Complete Second Factor Verification**
   
   
   
    Attempt to verify the user's second factor authentication details if MFA is required.



## Constructors


| | |
|---|---|
| [SignIn](-sign-in.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), status: [SignIn.Status](-status/index.html), supportedIdentifiers: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null, identifier: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, supportedFirstFactors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Factor](../../com.clerk.model.factor/-factor/index.html)&gt;? = null, supportedSecondFactors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Factor](../../com.clerk.model.factor/-factor/index.html)&gt;? = null, firstFactorVerification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, secondFactorVerification: [Verification](../../com.clerk.model.verification/-verification/index.html)? = null, userData: [SignIn.UserData](-user-data/index.html)? = null, createdSessionId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null) |


## Types


| Name | Summary |
|---|---|
| [AttemptFirstFactorParams](-attempt-first-factor-params/index.html) | [androidJvm]<br>sealed interface [AttemptFirstFactorParams](-attempt-first-factor-params/index.html)<br>A sealed interface defining parameter objects for attempting first factor verification in the sign-in process. |
| [AuthenticateWithRedirectParams](-authenticate-with-redirect-params/index.html) | [androidJvm]<br>sealed interface [AuthenticateWithRedirectParams](-authenticate-with-redirect-params/index.html)<br>A sealed interface defining parameter objects for redirect-based authentication strategies. |
| [Companion](-companion/index.html) | [androidJvm]<br>object [Companion](-companion/index.html) |
| [PrepareFirstFactorParams](-prepare-first-factor-params/index.html) | [androidJvm]<br>sealed interface [PrepareFirstFactorParams](-prepare-first-factor-params/index.html)<br>A sealed interface defining parameter objects for preparing first factor verification. |
| [PrepareSecondFactorParams](-prepare-second-factor-params/index.html) | [androidJvm]<br>@Serializable<br>data class [PrepareSecondFactorParams](-prepare-second-factor-params/index.html)(val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html))<br>A parameter object for preparing the second factor verification. |
| [ResetPasswordParams](-reset-password-params/index.html) | [androidJvm]<br>@Serializable<br>data class [ResetPasswordParams](-reset-password-params/index.html)(val password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val signOutOfOtherSessions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false)<br>Parameters for resetting a user's password during the sign-in process. |
| [SignInCreateParams](-sign-in-create-params/index.html) | [androidJvm]<br>object [SignInCreateParams](-sign-in-create-params/index.html)<br>Container object for sign-in creation parameters and strategies. |
| [Status](-status/index.html) | [androidJvm]<br>@Serializable<br>enum [Status](-status/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[SignIn.Status](-status/index.html)&gt; <br>Represents the status of a sign-in process. |
| [UserData](-user-data/index.html) | [androidJvm]<br>@Serializable<br>data class [UserData](-user-data/index.html)(val firstName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val lastName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val imageUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val hasImage: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null)<br>An object containing information about the user of the current sign-in. This property is populated only once an identifier is given to the SignIn object. |


## Properties


| Name | Summary |
|---|---|
| [createdSessionId](created-session-id.html) | [androidJvm]<br>@SerialName(value = &quot;created_session_id&quot;)<br>val [createdSessionId](created-session-id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The identifier of the session that was created upon completion of the current sign-in. |
| [firstFactorVerification](first-factor-verification.html) | [androidJvm]<br>@SerialName(value = &quot;first_factor_verification&quot;)<br>val [firstFactorVerification](first-factor-verification.html): [Verification](../../com.clerk.model.verification/-verification/index.html)? = null<br>The state of the verification process for the selected first factor. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>Unique identifier for this sign in. |
| [identifier](identifier.html) | [androidJvm]<br>val [identifier](identifier.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The authentication identifier value for the current sign-in. |
| [secondFactorVerification](second-factor-verification.html) | [androidJvm]<br>@SerialName(value = &quot;second_factor_verification&quot;)<br>val [secondFactorVerification](second-factor-verification.html): [Verification](../../com.clerk.model.verification/-verification/index.html)? = null<br>The state of the verification process for the selected second factor. |
| [status](status.html) | [androidJvm]<br>val [status](status.html): [SignIn.Status](-status/index.html)<br>The status of the current sign-in. |
| [supportedFirstFactors](supported-first-factors.html) | [androidJvm]<br>@SerialName(value = &quot;supported_first_factors&quot;)<br>val [supportedFirstFactors](supported-first-factors.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Factor](../../com.clerk.model.factor/-factor/index.html)&gt;? = null<br>Array of the first factors that are supported in the current sign-in. |
| [supportedIdentifiers](supported-identifiers.html) | [androidJvm]<br>@SerialName(value = &quot;supported_identifiers&quot;)<br>val [supportedIdentifiers](supported-identifiers.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;? = null<br>Array of all the authentication identifiers that are supported for this sign in. |
| [supportedSecondFactors](supported-second-factors.html) | [androidJvm]<br>@SerialName(value = &quot;supported_second_factors&quot;)<br>val [supportedSecondFactors](supported-second-factors.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Factor](../../com.clerk.model.factor/-factor/index.html)&gt;? = null<br>Array of the second factors that are supported in the current sign-in. |
| [userData](user-data.html) | [androidJvm]<br>@SerialName(value = &quot;user_data&quot;)<br>val [userData](user-data.html): [SignIn.UserData](-user-data/index.html)? = null<br>An object containing information about the user of the current sign-in. |


## Functions


| Name | Summary |
|---|---|
| [attemptFirstFactor](../attempt-first-factor.html) | [androidJvm]<br>suspend fun [SignIn](index.html).[attemptFirstFactor](../attempt-first-factor.html)(params: [SignIn.AttemptFirstFactorParams](-attempt-first-factor-params/index.html)): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Attempts to complete the first factor verification process. This is a required step in order to complete a sign in, as users should be verified at least by one factor of authentication. |
| [get](../get.html) | [androidJvm]<br>suspend fun [SignIn](index.html).[get](../get.html)(rotatingTokenNonce: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Retrieves the current state of the SignIn object from the server. |
| [prepareFirstFactor](../prepare-first-factor.html) | [androidJvm]<br>suspend fun [SignIn](index.html).[prepareFirstFactor](../prepare-first-factor.html)(strategy: [SignIn.PrepareFirstFactorParams.Strategy](-prepare-first-factor-params/-strategy/index.html)): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Begins the first factor verification process. This is a required step in order to complete a sign in, as users should be verified at least by one factor of authentication. |
| [resetPassword](../reset-password.html) | [androidJvm]<br>suspend fun [SignIn](index.html).[resetPassword](../reset-password.html)(params: [SignIn.ResetPasswordParams](-reset-password-params/index.html)): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>suspend fun [SignIn](index.html).[resetPassword](../reset-password.html)(password: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), signOutOfOtherSessions: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false): [ClerkApiResult](../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SignIn](index.html), [ClerkErrorResponse](../../com.clerk.model.error/-clerk-error-response/index.html)&gt;<br>Resets the password for the current sign in attempt. |
| [toSSOResult](../to-s-s-o-result.html) | [androidJvm]<br>fun [SignIn](index.html).[toSSOResult](../to-s-s-o-result.html)(): [SSOResult](../../com.clerk.sso/-s-s-o-result/index.html)<br>Converts the current [SignIn](index.html) instance to an [SSOResult](../../com.clerk.sso/-s-s-o-result/index.html). |

