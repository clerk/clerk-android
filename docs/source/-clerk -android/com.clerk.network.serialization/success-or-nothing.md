---
title: successOrNothing
---
//[Clerk Android](../../index.html)/[com.clerk.network.serialization](index.html)/[successOrNothing](success-or-nothing.html)



# successOrNothing



[androidJvm]\
inline fun &lt;[T](success-or-nothing.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](success-or-nothing.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](success-or-nothing.html), [E](success-or-nothing.html)&gt;.[successOrNothing](success-or-nothing.html)(body: (failure: [ClerkApiResult.Failure](-clerk-api-result/-failure/index.html)&lt;[E](success-or-nothing.html)&gt;) -&gt; [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-nothing/index.html)): [T](success-or-nothing.html)



If [ClerkApiResult.Success](-clerk-api-result/-success/index.html), returns the underlying [T](success-or-nothing.html) value. Otherwise, calls [body](success-or-nothing.html) with the failure, which can either throw an exception or return early (since this function is inline).




