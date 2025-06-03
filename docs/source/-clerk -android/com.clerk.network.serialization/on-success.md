---
title: onSuccess
---
//[Clerk Android](../../index.html)/[com.clerk.network.serialization](index.html)/[onSuccess](on-success.html)



# onSuccess



[androidJvm]\
inline fun &lt;[T](on-success.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](on-success.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](on-success.html), [E](on-success.html)&gt;.[onSuccess](on-success.html)(action: (value: [T](on-success.html)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-unit/index.html)): [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](on-success.html), [E](on-success.html)&gt;



Performs the given [action](on-success.html) on the encapsulated value if this instance represents [success](-clerk-api-result/-success/index.html). Returns the original `ClerkApiResult` unchanged.




