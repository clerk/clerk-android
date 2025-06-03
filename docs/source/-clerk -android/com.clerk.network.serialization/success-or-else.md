---
title: successOrElse
---
//[Clerk Android](../../index.html)/[com.clerk.network.serialization](index.html)/[successOrElse](success-or-else.html)



# successOrElse



[androidJvm]\
inline fun &lt;[T](success-or-else.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](success-or-else.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](success-or-else.html), [E](success-or-else.html)&gt;.[successOrElse](success-or-else.html)(defaultValue: (failure: [ClerkApiResult.Failure](-clerk-api-result/-failure/index.html)&lt;[E](success-or-else.html)&gt;) -&gt; [T](success-or-else.html)): [T](success-or-else.html)



If [ClerkApiResult.Success](-clerk-api-result/-success/index.html), returns the underlying [T](success-or-else.html) value. Otherwise, returns the result of the [defaultValue](success-or-else.html) function.




