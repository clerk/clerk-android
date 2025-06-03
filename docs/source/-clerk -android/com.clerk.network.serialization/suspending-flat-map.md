---
title: suspendingFlatMap
---
//[Clerk Android](../../index.html)/[com.clerk.network.serialization](index.html)/[suspendingFlatMap](suspending-flat-map.html)



# suspendingFlatMap



[androidJvm]\
inline suspend fun &lt;[T](suspending-flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [R](suspending-flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](suspending-flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](suspending-flat-map.html), [E](suspending-flat-map.html)&gt;.[suspendingFlatMap](suspending-flat-map.html)(transform: suspend (value: [T](suspending-flat-map.html)) -&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[R](suspending-flat-map.html), [E](suspending-flat-map.html)&gt;): [ClerkApiResult](-clerk-api-result/index.html)&lt;[R](suspending-flat-map.html), [E](suspending-flat-map.html)&gt;



Returns a new [ClerkApiResult](-clerk-api-result/index.html) by applying [transform](suspending-flat-map.html) to the value of a [ClerkApiResult.Success](-clerk-api-result/-success/index.html), or returns the original [ClerkApiResult.Failure](-clerk-api-result/-failure/index.html) if this is a failure.




