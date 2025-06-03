---
title: onFailureType
---
//[Clerk Android](../../index.html)/[com.clerk.network.serialization](index.html)/[onFailureType](on-failure-type.html)



# onFailureType



[androidJvm]\
inline fun &lt;[T](on-failure-type.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](on-failure-type.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](on-failure-type.html), [E](on-failure-type.html)&gt;.[onFailureType](on-failure-type.html)(errorType: [ClerkApiResult.Failure.ErrorType](-clerk-api-result/-failure/-error-type/index.html), action: (failure: [ClerkApiResult.Failure](-clerk-api-result/-failure/index.html)&lt;[E](on-failure-type.html)&gt;) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)): [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](on-failure-type.html), [E](on-failure-type.html)&gt;



Performs the given [action](on-failure-type.html) on the encapsulated failure if this instance represents a failure with the specified error type. Returns the original `ClerkApiResult` unchanged.




