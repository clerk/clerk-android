---
title: FraudSettings
---
//[Clerk Android](../../../index.html)/[com.clerk.model.environment](../index.html)/[FraudSettings](index.html)



# FraudSettings



[androidJvm]\
@Serializable



data class [FraudSettings](index.html)(val native: [FraudSettings.Native](-native/index.html))

Settings for fraud prevention in the Clerk environment.



## Constructors


| | |
|---|---|
| [FraudSettings](-fraud-settings.html) | [androidJvm]<br>constructor(native: [FraudSettings.Native](-native/index.html)) |


## Types


| Name | Summary |
|---|---|
| [DeviceAttestationMode](-device-attestation-mode/index.html) | [androidJvm]<br>@Serializable<br>enum [DeviceAttestationMode](-device-attestation-mode/index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[FraudSettings.DeviceAttestationMode](-device-attestation-mode/index.html)&gt; <br>Enum representing the device attestation mode. |
| [Native](-native/index.html) | [androidJvm]<br>@Serializable<br>data class [Native](-native/index.html)(val deviceAttestationMode: [FraudSettings.DeviceAttestationMode](-device-attestation-mode/index.html))<br>Native platform specific fraud prevention settings. |


## Properties


| Name | Summary |
|---|---|
| [native](native.html) | [androidJvm]<br>val [native](native.html): [FraudSettings.Native](-native/index.html) |

