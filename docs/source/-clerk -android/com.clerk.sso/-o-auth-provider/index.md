---
title: OAuthProvider
---
//[Clerk Android](../../../index.html)/[com.clerk.sso](../index.html)/[OAuthProvider](index.html)



# OAuthProvider



[androidJvm]\
enum [OAuthProvider](index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[OAuthProvider](index.html)&gt; 

Enum class representing supported OAuth providers for authentication. Each provider is associated with a specific strategy string used in Clerk API requests.



## Entries


| | |
|---|---|
| [FACEBOOK](-f-a-c-e-b-o-o-k/index.html) | [androidJvm]<br>[FACEBOOK](-f-a-c-e-b-o-o-k/index.html) |
| [GOOGLE](-g-o-o-g-l-e/index.html) | [androidJvm]<br>[GOOGLE](-g-o-o-g-l-e/index.html) |
| [HUBSPOT](-h-u-b-s-p-o-t/index.html) | [androidJvm]<br>[HUBSPOT](-h-u-b-s-p-o-t/index.html) |
| [GITHUB](-g-i-t-h-u-b/index.html) | [androidJvm]<br>[GITHUB](-g-i-t-h-u-b/index.html) |
| [TIKTOK](-t-i-k-t-o-k/index.html) | [androidJvm]<br>[TIKTOK](-t-i-k-t-o-k/index.html) |
| [GITLAB](-g-i-t-l-a-b/index.html) | [androidJvm]<br>[GITLAB](-g-i-t-l-a-b/index.html) |
| [DISCORD](-d-i-s-c-o-r-d/index.html) | [androidJvm]<br>[DISCORD](-d-i-s-c-o-r-d/index.html) |
| [TWITTER](-t-w-i-t-t-e-r/index.html) | [androidJvm]<br>[TWITTER](-t-w-i-t-t-e-r/index.html) |
| [TWITCH](-t-w-i-t-c-h/index.html) | [androidJvm]<br>[TWITCH](-t-w-i-t-c-h/index.html) |
| [LINKEDIN](-l-i-n-k-e-d-i-n/index.html) | [androidJvm]<br>[LINKEDIN](-l-i-n-k-e-d-i-n/index.html) |
| [LINKEDIN_OIDC](-l-i-n-k-e-d-i-n_-o-i-d-c/index.html) | [androidJvm]<br>[LINKEDIN_OIDC](-l-i-n-k-e-d-i-n_-o-i-d-c/index.html) |
| [DROPBOX](-d-r-o-p-b-o-x/index.html) | [androidJvm]<br>[DROPBOX](-d-r-o-p-b-o-x/index.html) |
| [ATLASSIAN](-a-t-l-a-s-s-i-a-n/index.html) | [androidJvm]<br>[ATLASSIAN](-a-t-l-a-s-s-i-a-n/index.html) |
| [BITBUCKET](-b-i-t-b-u-c-k-e-t/index.html) | [androidJvm]<br>[BITBUCKET](-b-i-t-b-u-c-k-e-t/index.html) |
| [MICROSOFT](-m-i-c-r-o-s-o-f-t/index.html) | [androidJvm]<br>[MICROSOFT](-m-i-c-r-o-s-o-f-t/index.html) |
| [NOTION](-n-o-t-i-o-n/index.html) | [androidJvm]<br>[NOTION](-n-o-t-i-o-n/index.html) |
| [APPLE](-a-p-p-l-e/index.html) | [androidJvm]<br>[APPLE](-a-p-p-l-e/index.html) |
| [LINE](-l-i-n-e/index.html) | [androidJvm]<br>[LINE](-l-i-n-e/index.html) |
| [INSTAGRAM](-i-n-s-t-a-g-r-a-m/index.html) | [androidJvm]<br>[INSTAGRAM](-i-n-s-t-a-g-r-a-m/index.html) |
| [COINBASE](-c-o-i-n-b-a-s-e/index.html) | [androidJvm]<br>[COINBASE](-c-o-i-n-b-a-s-e/index.html) |
| [SPOTIFY](-s-p-o-t-i-f-y/index.html) | [androidJvm]<br>[SPOTIFY](-s-p-o-t-i-f-y/index.html) |
| [XERO](-x-e-r-o/index.html) | [androidJvm]<br>[XERO](-x-e-r-o/index.html) |
| [BOX](-b-o-x/index.html) | [androidJvm]<br>[BOX](-b-o-x/index.html) |
| [SLACK](-s-l-a-c-k/index.html) | [androidJvm]<br>[SLACK](-s-l-a-c-k/index.html) |
| [LINEAR](-l-i-n-e-a-r/index.html) | [androidJvm]<br>[LINEAR](-l-i-n-e-a-r/index.html) |
| [HUGGING_FACE](-h-u-g-g-i-n-g_-f-a-c-e/index.html) | [androidJvm]<br>[HUGGING_FACE](-h-u-g-g-i-n-g_-f-a-c-e/index.html) |
| [CUSTOM](-c-u-s-t-o-m/index.html) | [androidJvm]<br>[CUSTOM](-c-u-s-t-o-m/index.html) |


## Types


| Name | Summary |
|---|---|
| [Companion](-companion/index.html) | [androidJvm]<br>object [Companion](-companion/index.html) |


## Properties


| Name | Summary |
|---|---|
| [entries](entries.html) | [androidJvm]<br>val [entries](entries.html): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[OAuthProvider](index.html)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |
| [strategy](strategy.html) | [androidJvm]<br>val [strategy](strategy.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>property holds the API-specific identifier for the provider. |


## Functions


| Name | Summary |
|---|---|
| [valueOf](value-of.html) | [androidJvm]<br>fun [valueOf](value-of.html)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [OAuthProvider](index.html)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.html) | [androidJvm]<br>fun [values](values.html)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[OAuthProvider](index.html)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |

