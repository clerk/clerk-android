---
title: SSOResult
---
//[Clerk Android](../../../index.html)/[com.clerk.sso](../index.html)/[SSOResult](index.html)



# SSOResult



[androidJvm]\
@Serializable



data class [SSOResult](index.html)(val signIn: [SignIn](../../com.clerk.signin/-sign-in/index.html)? = null, val signUp: [SignUp](../../com.clerk.signup/-sign-up/index.html)? = null)

The result of an SSO operation.



## Constructors


| | |
|---|---|
| [SSOResult](-s-s-o-result.html) | [androidJvm]<br>constructor(signIn: [SignIn](../../com.clerk.signin/-sign-in/index.html)? = null, signUp: [SignUp](../../com.clerk.signup/-sign-up/index.html)? = null) |


## Properties


| Name | Summary |
|---|---|
| [resultType](result-type.html) | [androidJvm]<br>val [resultType](result-type.html): [ResultType](../-result-type/index.html)<br>Convenience property to determine the type of result. |
| [signIn](sign-in.html) | [androidJvm]<br>val [signIn](sign-in.html): [SignIn](../../com.clerk.signin/-sign-in/index.html)? = null<br>The sign-in object if the SSO operation resulted in a sign-in. |
| [signUp](sign-up.html) | [androidJvm]<br>val [signUp](sign-up.html): [SignUp](../../com.clerk.signup/-sign-up/index.html)? = null<br>The sign-up object if the SSO operation resulted in a sign-up. |

