---
title: InstanceEnvironmentType
---
//[Clerk Android](../../../index.html)/[com.clerk.model.environment](../index.html)/[InstanceEnvironmentType](index.html)



# InstanceEnvironmentType



[androidJvm]\
@Serializable



enum [InstanceEnvironmentType](index.html) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-enum/index.html)&lt;[InstanceEnvironmentType](index.html)&gt; 

An enumeration representing the type of environment for an instance.



This is used to distinguish between production and development environments, allowing for environment-specific configurations and behaviors.



## Entries


| | |
|---|---|
| [PRODUCTION](-p-r-o-d-u-c-t-i-o-n/index.html) | [androidJvm]<br>@SerialName(value = &quot;production&quot;)<br>[PRODUCTION](-p-r-o-d-u-c-t-i-o-n/index.html)<br>Represents a production environment. |
| [DEVELOPMENT](-d-e-v-e-l-o-p-m-e-n-t/index.html) | [androidJvm]<br>@SerialName(value = &quot;development&quot;)<br>[DEVELOPMENT](-d-e-v-e-l-o-p-m-e-n-t/index.html)<br>Represents a development environment. |
| [UNKNOWN](-u-n-k-n-o-w-n/index.html) | [androidJvm]<br>@SerialName(value = &quot;unknown&quot;)<br>[UNKNOWN](-u-n-k-n-o-w-n/index.html)<br>Used as a fallback in case of decoding error. |


## Properties


| Name | Summary |
|---|---|
| [entries](entries.html) | [androidJvm]<br>val [entries](entries.html): [EnumEntries](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.enums/-enum-entries/index.html)&lt;[InstanceEnvironmentType](index.html)&gt;<br>Returns a representation of an immutable list of all enum entries, in the order they're declared. |


## Functions


| Name | Summary |
|---|---|
| [valueOf](value-of.html) | [androidJvm]<br>fun [valueOf](value-of.html)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html)): [InstanceEnvironmentType](index.html)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.html) | [androidJvm]<br>fun [values](values.html)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[InstanceEnvironmentType](index.html)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |

