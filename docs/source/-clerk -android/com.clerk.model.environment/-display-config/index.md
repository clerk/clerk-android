---
title: DisplayConfig
---
//[Clerk Android](../../../index.html)/[com.clerk.model.environment](../index.html)/[DisplayConfig](index.html)



# DisplayConfig



[androidJvm]\
@Serializable



data class [DisplayConfig](index.html)(val instanceEnvironmentType: [InstanceEnvironmentType](../-instance-environment-type/index.html), val applicationName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val preferredSignInStrategy: [PreferredSignInStrategy](../-preferred-sign-in-strategy/index.html), val branded: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val logoImageUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val homeUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val privacyPolicyUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?, val termsUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?)



## Constructors


| | |
|---|---|
| [DisplayConfig](-display-config.html) | [androidJvm]<br>constructor(instanceEnvironmentType: [InstanceEnvironmentType](../-instance-environment-type/index.html), applicationName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), preferredSignInStrategy: [PreferredSignInStrategy](../-preferred-sign-in-strategy/index.html), branded: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), logoImageUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), homeUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), privacyPolicyUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?, termsUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) |


## Properties


| Name | Summary |
|---|---|
| [applicationName](application-name.html) | [androidJvm]<br>@SerialName(value = &quot;application_name&quot;)<br>val [applicationName](application-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [branded](branded.html) | [androidJvm]<br>@SerialName(value = &quot;branded&quot;)<br>val [branded](branded.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [homeUrl](home-url.html) | [androidJvm]<br>@SerialName(value = &quot;home_url&quot;)<br>val [homeUrl](home-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [instanceEnvironmentType](instance-environment-type.html) | [androidJvm]<br>@SerialName(value = &quot;instance_environment_type&quot;)<br>val [instanceEnvironmentType](instance-environment-type.html): [InstanceEnvironmentType](../-instance-environment-type/index.html) |
| [logoImageUrl](logo-image-url.html) | [androidJvm]<br>@SerialName(value = &quot;logo_image_url&quot;)<br>val [logoImageUrl](logo-image-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html) |
| [preferredSignInStrategy](preferred-sign-in-strategy.html) | [androidJvm]<br>@SerialName(value = &quot;preferred_sign_in_strategy&quot;)<br>val [preferredSignInStrategy](preferred-sign-in-strategy.html): [PreferredSignInStrategy](../-preferred-sign-in-strategy/index.html) |
| [privacyPolicyUrl](privacy-policy-url.html) | [androidJvm]<br>@SerialName(value = &quot;privacy_policy_url&quot;)<br>val [privacyPolicyUrl](privacy-policy-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? |
| [termsUrl](terms-url.html) | [androidJvm]<br>@SerialName(value = &quot;terms_url&quot;)<br>val [termsUrl](terms-url.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? |

