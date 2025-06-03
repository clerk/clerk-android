---
title: flatMap
---
//[Clerk Android](../../index.html)/[com.clerk.network.serialization](index.html)/[flatMap](flat-map.html)



# flatMap



[androidJvm]\
inline fun &lt;[T](flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [R](flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](flat-map.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](flat-map.html), [E](flat-map.html)&gt;.[flatMap](flat-map.html)(transform: (value: [T](flat-map.html)) -&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[R](flat-map.html), [E](flat-map.html)&gt;): [ClerkApiResult](-clerk-api-result/index.html)&lt;[R](flat-map.html), [E](flat-map.html)&gt;



Returns a new [ClerkApiResult](-clerk-api-result/index.html) by applying [transform](flat-map.html) to the value of a [ClerkApiResult.Success](-clerk-api-result/-success/index.html), or returns the original [ClerkApiResult.Failure](-clerk-api-result/-failure/index.html) if this is a failure.




