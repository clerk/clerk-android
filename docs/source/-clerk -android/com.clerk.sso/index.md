---
title: com.clerk.sso
---
//[Clerk Android](../../index.html)/[com.clerk.sso](index.html)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [OAuthProvider](-o-auth-provider/index.html) | [androidJvm]<br>enum [OAuthProvider](-o-auth-provider/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[OAuthProvider](-o-auth-provider/index.html)&gt; <br>Enum class representing supported OAuth providers for authentication. Each provider is associated with a specific strategy string used in Clerk API requests. |
| [RedirectConfiguration](-redirect-configuration/index.html) | [androidJvm]<br>object [RedirectConfiguration](-redirect-configuration/index.html) |
| [ResultType](-result-type/index.html) | [androidJvm]<br>enum [ResultType](-result-type/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[ResultType](-result-type/index.html)&gt; |
| [SSOResult](-s-s-o-result/index.html) | [androidJvm]<br>@Serializable<br>data class [SSOResult](-s-s-o-result/index.html)(val signIn: [SignIn](../com.clerk.signin/-sign-in/index.html)? = null, val signUp: [SignUp](../com.clerk.signup/-sign-up/index.html)? = null)<br>The result of an SSO operation. |

