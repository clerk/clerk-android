---
title: onFailure
---
//[Clerk Android](../../index.html)/[com.clerk.network.serialization](index.html)/[onFailure](on-failure.html)



# onFailure



[androidJvm]\
inline fun &lt;[T](on-failure.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](on-failure.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](on-failure.html), [E](on-failure.html)&gt;.[onFailure](on-failure.html)(action: (failure: [ClerkApiResult.Failure](-clerk-api-result/-failure/index.html)&lt;[E](on-failure.html)&gt;) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)): [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](on-failure.html), [E](on-failure.html)&gt;



Performs the given [action](on-failure.html) on the encapsulated [ClerkApiResult.Failure](-clerk-api-result/-failure/index.html) if this instance represents [failure](-clerk-api-result/-failure/index.html). Returns the original `ClerkApiResult` unchanged.




