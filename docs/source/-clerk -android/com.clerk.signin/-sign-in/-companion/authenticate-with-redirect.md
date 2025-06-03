---
title: authenticateWithRedirect
---
//[Clerk Android](../../../../index.html)/[com.clerk.signin](../../index.html)/[SignIn](../index.html)/[Companion](index.html)/[authenticateWithRedirect](authenticate-with-redirect.html)



# authenticateWithRedirect



[androidJvm]\
suspend fun [authenticateWithRedirect](authenticate-with-redirect.html)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), params: [SignIn.AuthenticateWithRedirectParams](../-authenticate-with-redirect-params/index.html)): [ClerkApiResult](../../../com.clerk.network.serialization/-clerk-api-result/index.html)&lt;[SSOResult](../../../com.clerk.sso/-s-s-o-result/index.html), [ClerkErrorResponse](../../../com.clerk.model.error/-clerk-error-response/index.html)&gt;



Initiates the sign-in process using an OAuth or Enterprise SSO redirect flow.



This method is used for authentication strategies that require redirecting the user to an external authentication provider (e.g., Google, Facebook, or an Enterprise SSO provider). The user will be redirected to the specified [AuthenticateWithRedirectParams.redirectUrl](../-authenticate-with-redirect-params/redirect-url.html) to complete authentication.



#### Return



A [ClerkApiResult](../../../com.clerk.network.serialization/-clerk-api-result/index.html) containing the result of the authentication flow. The [SSOResult](../../../com.clerk.sso/-s-s-o-result/index.html) could contain either a sign-in or sign-up result, depending on whether an account transfer took place (i.e. if the user didn't have an account and a sign up was created instead).



**See Also:**[OAuthProviders](https://clerk.com/docs/references/javascript/types/sso) \n \n Example usage:

```kotlin
SignIn.authenticateWithRedirect(context, AuthenticateWithRedirectParams(provider = OAuthProvider.GOOGLE))
  .onSuccess { result ->  // Handle the result }
  .onFailure { error ->  // Handle the error }
```


#### Parameters


androidJvm

| | |
|---|---|
| context | The context in which the authentication flow is initiated. Used to open the in app browser. |
| params | The parameters for the redirect-based authentication. [AuthenticateWithRedirectParams.provider](../-authenticate-with-redirect-params/provider.html) an [OAuthProvider](../-authenticate-with-redirect-params/redirect-url.html) The URL to redirect the user to after initiating the authentication flow. Set by default to [RedirectConfiguration.REDIRECT_URL](../../../com.clerk.sso/-redirect-configuration/-r-e-d-i-r-e-c-t_-u-r-l.html) |




