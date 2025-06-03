---
title: SessionActivity
---
//[Clerk Android](../../../index.html)/[com.clerk.model.session](../index.html)/[SessionActivity](index.html)



# SessionActivity



[androidJvm]\
@Serializable



data class [SessionActivity](index.html)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), val browserName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val browserVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val deviceType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val ipAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val city: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val country: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, val isMobile: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null)

A `SessionActivity` object will provide information about the user's location, device and browser.



## Constructors


| | |
|---|---|
| [SessionActivity](-session-activity.html) | [androidJvm]<br>constructor(id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), browserName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, browserVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, deviceType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, ipAddress: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, city: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, country: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null, isMobile: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null) |


## Properties


| Name | Summary |
|---|---|
| [browserName](browser-name.html) | [androidJvm]<br>@SerialName(value = &quot;browser_name&quot;)<br>val [browserName](browser-name.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The name of the browser from which this session activity occurred. |
| [browserVersion](browser-version.html) | [androidJvm]<br>@SerialName(value = &quot;browser_version&quot;)<br>val [browserVersion](browser-version.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The version of the browser from which this session activity occurred. |
| [city](city.html) | [androidJvm]<br>val [city](city.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The city from which this session activity occurred. Resolved by IP address geo-location. |
| [country](country.html) | [androidJvm]<br>val [country](country.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The country from which this session activity occurred. Resolved by IP address geo-location. |
| [deviceType](device-type.html) | [androidJvm]<br>@SerialName(value = &quot;device_type&quot;)<br>val [deviceType](device-type.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The type of the device which was used in this session activity. |
| [id](id.html) | [androidJvm]<br>val [id](id.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)<br>A unique identifier for the session activity record. |
| [ipAddress](ip-address.html) | [androidJvm]<br>@SerialName(value = &quot;ip_address&quot;)<br>val [ipAddress](ip-address.html): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)? = null<br>The IP address from which this session activity originated. |
| [isMobile](is-mobile.html) | [androidJvm]<br>@SerialName(value = &quot;is_mobile&quot;)<br>val [isMobile](is-mobile.html): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)? = null<br>Will be set to true if the session activity came from a mobile device. Set to false otherwise. |

