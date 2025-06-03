---
title: UserSettings
---
//[Clerk Android](../../../index.html)/[com.clerk.model.environment](../index.html)/[UserSettings](index.html)



# UserSettings



[androidJvm]\
@Serializable



data class [UserSettings](index.html)(val attributes: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [UserSettings.AttributesConfig](-attributes-config/index.html)&gt;, val signUp: [UserSettings.SignUpUserSettings](-sign-up-user-settings/index.html), val social: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [UserSettings.SocialConfig](-social-config/index.html)&gt;, val actions: [UserSettings.Actions](-actions/index.html), val passkeySettings: [UserSettings.PasskeySettings](-passkey-settings/index.html)?)



## Constructors


| | |
|---|---|
| [UserSettings](-user-settings.html) | [androidJvm]<br>constructor(attributes: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [UserSettings.AttributesConfig](-attributes-config/index.html)&gt;, signUp: [UserSettings.SignUpUserSettings](-sign-up-user-settings/index.html), social: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [UserSettings.SocialConfig](-social-config/index.html)&gt;, actions: [UserSettings.Actions](-actions/index.html), passkeySettings: [UserSettings.PasskeySettings](-passkey-settings/index.html)?) |


## Types


| Name | Summary |
|---|---|
| [Actions](-actions/index.html) | [androidJvm]<br>@Serializable<br>data class [Actions](-actions/index.html)(val deleteSelf: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false, val createOrganization: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false) |
| [AttributesConfig](-attributes-config/index.html) | [androidJvm]<br>@Serializable<br>data class [AttributesConfig](-attributes-config/index.html)(val enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val required: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val usedForFirstFactor: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val firstFactors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;?, val usedForSecondFactor: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val secondFactors: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;?, val verifications: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)&gt;?, val verifyAtSignUp: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)) |
| [PasskeySettings](-passkey-settings/index.html) | [androidJvm]<br>@Serializable<br>data class [PasskeySettings](-passkey-settings/index.html)(val allowAutofill: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val showSignInButton: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)) |
| [SignUpUserSettings](-sign-up-user-settings/index.html) | [androidJvm]<br>@Serializable<br>data class [SignUpUserSettings](-sign-up-user-settings/index.html)(val customActionRequired: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val progressive: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val mode: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val legalConsentEnabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)) |
| [SocialConfig](-social-config/index.html) | [androidJvm]<br>@Serializable<br>data class [SocialConfig](-social-config/index.html)(val enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val required: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val authenticatable: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val strategy: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val notSelectable: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val logoUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)?) |


## Properties


| Name | Summary |
|---|---|
| [actions](actions.html) | [androidJvm]<br>val [actions](actions.html): [UserSettings.Actions](-actions/index.html) |
| [attributes](attributes.html) | [androidJvm]<br>val [attributes](attributes.html): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [UserSettings.AttributesConfig](-attributes-config/index.html)&gt; |
| [passkeySettings](passkey-settings.html) | [androidJvm]<br>@SerialName(value = &quot;passkey_settings&quot;)<br>val [passkeySettings](passkey-settings.html): [UserSettings.PasskeySettings](-passkey-settings/index.html)? |
| [signUp](sign-up.html) | [androidJvm]<br>@SerialName(value = &quot;sign_up&quot;)<br>val [signUp](sign-up.html): [UserSettings.SignUpUserSettings](-sign-up-user-settings/index.html) |
| [social](social.html) | [androidJvm]<br>val [social](social.html): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), [UserSettings.SocialConfig](-social-config/index.html)&gt; |

