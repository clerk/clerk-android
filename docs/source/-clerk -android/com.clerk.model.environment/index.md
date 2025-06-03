---
title: com.clerk.model.environment
---
//[Clerk Android](../../index.html)/[com.clerk.model.environment](index.html)



# Package-level declarations



## Types


| Name | Summary |
|---|---|
| [AuthConfig](-auth-config/index.html) | [androidJvm]<br>@Serializable<br>data class [AuthConfig](-auth-config/index.html)(val singleSessionMode: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)) |
| [DisplayConfig](-display-config/index.html) | [androidJvm]<br>@Serializable<br>data class [DisplayConfig](-display-config/index.html)(val instanceEnvironmentType: [InstanceEnvironmentType](-instance-environment-type/index.html), val applicationName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val preferredSignInStrategy: [PreferredSignInStrategy](-preferred-sign-in-strategy/index.html), val branded: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val logoImageUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val homeUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val privacyPolicyUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?, val termsUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) |
| [FraudSettings](-fraud-settings/index.html) | [androidJvm]<br>@Serializable<br>data class [FraudSettings](-fraud-settings/index.html)(val native: [FraudSettings.Native](-fraud-settings/-native/index.html))<br>Settings for fraud prevention in the Clerk environment. |
| [InstanceEnvironmentType](-instance-environment-type/index.html) | [androidJvm]<br>@Serializable<br>enum [InstanceEnvironmentType](-instance-environment-type/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[InstanceEnvironmentType](-instance-environment-type/index.html)&gt; <br>An enumeration representing the type of environment for an instance. |
| [PreferredSignInStrategy](-preferred-sign-in-strategy/index.html) | [androidJvm]<br>@Serializable<br>enum [PreferredSignInStrategy](-preferred-sign-in-strategy/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[PreferredSignInStrategy](-preferred-sign-in-strategy/index.html)&gt; |
| [UserSettings](-user-settings/index.html) | [androidJvm]<br>@Serializable<br>data class [UserSettings](-user-settings/index.html)(val attributes: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [UserSettings.AttributesConfig](-user-settings/-attributes-config/index.html)&gt;, val signUp: [UserSettings.SignUpUserSettings](-user-settings/-sign-up-user-settings/index.html), val social: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [UserSettings.SocialConfig](-user-settings/-social-config/index.html)&gt;, val actions: [UserSettings.Actions](-user-settings/-actions/index.html), val passkeySettings: [UserSettings.PasskeySettings](-user-settings/-passkey-settings/index.html)?) |

