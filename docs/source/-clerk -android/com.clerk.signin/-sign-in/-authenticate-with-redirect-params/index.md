---
title: AuthenticateWithRedirectParams
---
//[Clerk Android](../../../../index.html)/[com.clerk.signin](../../index.html)/[SignIn](../index.html)/[AuthenticateWithRedirectParams](index.html)



# AuthenticateWithRedirectParams

sealed interface [AuthenticateWithRedirectParams](index.html)

A sealed interface defining parameter objects for redirect-based authentication strategies.



This includes OAuth providers and Enterprise SSO configurations that require redirecting the user to an external authentication provider.



#### Inheritors


| |
|---|
| [OAuth](-o-auth/index.html) |
| [EnterpriseSSO](-enterprise-s-s-o/index.html) |


## Types


| Name | Summary |
|---|---|
| [EnterpriseSSO](-enterprise-s-s-o/index.html) | [androidJvm]<br>data class [EnterpriseSSO](-enterprise-s-s-o/index.html)(val provider: [OAuthProvider](../../../com.clerk.sso/-o-auth-provider/index.html), val redirectUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = RedirectConfiguration.REDIRECT_URL) : [SignIn.AuthenticateWithRedirectParams](index.html)<br>Parameters for Enterprise SSO authentication with redirect. |
| [OAuth](-o-auth/index.html) | [androidJvm]<br>data class [OAuth](-o-auth/index.html)(val provider: [OAuthProvider](../../../com.clerk.sso/-o-auth-provider/index.html), val redirectUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = RedirectConfiguration.REDIRECT_URL) : [SignIn.AuthenticateWithRedirectParams](index.html)<br>Parameters for OAuth authentication with redirect. |


## Properties


| Name | Summary |
|---|---|
| [provider](provider.html) | [androidJvm]<br>abstract val [provider](provider.html): [OAuthProvider](../../../com.clerk.sso/-o-auth-provider/index.html)<br>The OAuth or SSO provider to authenticate with. |
| [redirectUrl](redirect-url.html) | [androidJvm]<br>abstract val [redirectUrl](redirect-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The URL to redirect to after the authentication flow completes. |

