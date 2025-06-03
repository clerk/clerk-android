---
title: supportedSecondFactors
---
//[Clerk Android](../../../index.html)/[com.clerk.signin](../index.html)/[SignIn](index.html)/[supportedSecondFactors](supported-second-factors.html)



# supportedSecondFactors



[androidJvm]\




@SerialName(value = &quot;supported_second_factors&quot;)



val [supportedSecondFactors](supported-second-factors.html): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin.collections/-list/index.html)&lt;[Factor](../../com.clerk.model.factor/-factor/index.html)&gt;? = null



Array of the second factors that are supported in the current sign-in.



Each factor contains information about the verification strategy that can be used. This property is populated only when the first factor is verified.




