---
title: OAuth
---
//[Clerk Android](../../../../../index.html)/[com.clerk.signin](../../../index.html)/[SignIn](../../index.html)/[AuthenticateWithRedirectParams](../index.html)/[OAuth](index.html)



# OAuth



[androidJvm]\
data class [OAuth](index.html)(val provider: [OAuthProvider](../../../../com.clerk.sso/-o-auth-provider/index.html), val redirectUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = RedirectConfiguration.REDIRECT_URL) : [SignIn.AuthenticateWithRedirectParams](../index.html)

Parameters for OAuth authentication with redirect.



## Constructors


| | |
|---|---|
| [OAuth](-o-auth.html) | [androidJvm]<br>constructor(provider: [OAuthProvider](../../../../com.clerk.sso/-o-auth-provider/index.html))constructor(provider: [OAuthProvider](../../../../com.clerk.sso/-o-auth-provider/index.html), redirectUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) = RedirectConfiguration.REDIRECT_URL) |


## Properties


| Name | Summary |
|---|---|
| [provider](provider.html) | [androidJvm]<br>open override val [provider](provider.html): [OAuthProvider](../../../../com.clerk.sso/-o-auth-provider/index.html)<br>The OAuth provider (e.g., Google, Facebook, GitHub). |
| [redirectUrl](redirect-url.html) | [androidJvm]<br>@SerialName(value = &quot;redirect_url&quot;)<br>open override val [redirectUrl](redirect-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>The URL to redirect to after OAuth completion. |

