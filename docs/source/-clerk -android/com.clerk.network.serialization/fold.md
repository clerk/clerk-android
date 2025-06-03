---
title: fold
---
//[Clerk Android](../../index.html)/[com.clerk.network.serialization](index.html)/[fold](fold.html)



# fold



[androidJvm]\
inline fun &lt;[T](fold.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](fold.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [C](fold.html)&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](fold.html), [E](fold.html)&gt;.[fold](fold.html)(onSuccess: (value: [T](fold.html)) -&gt; [C](fold.html), onFailure: (failure: [ClerkApiResult.Failure](-clerk-api-result/-failure/index.html)&lt;[E](fold.html)&gt;) -&gt; [C](fold.html)): [C](fold.html)



Transforms an [ClerkApiResult](-clerk-api-result/index.html) into a [C](fold.html) value.




