---
title: suspendingFold
---
//[Clerk Android](../../index.html)/[com.clerk.network.serialization](index.html)/[suspendingFold](suspending-fold.html)



# suspendingFold



[androidJvm]\
inline suspend fun &lt;[T](suspending-fold.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [E](suspending-fold.html) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), [C](suspending-fold.html)&gt; [ClerkApiResult](-clerk-api-result/index.html)&lt;[T](suspending-fold.html), [E](suspending-fold.html)&gt;.[suspendingFold](suspending-fold.html)(noinline onSuccess: suspend (value: [T](suspending-fold.html)) -&gt; [C](suspending-fold.html), noinline onFailure: (failure: [ClerkApiResult.Failure](-clerk-api-result/-failure/index.html)&lt;[E](suspending-fold.html)&gt;) -&gt; [C](suspending-fold.html)): [C](suspending-fold.html)



Transforms an [ClerkApiResult](-clerk-api-result/index.html) into a [C](suspending-fold.html) value.




