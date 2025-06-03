---
title: isInitialized
---
//[Clerk Android](../../../index.html)/[com.clerk](../index.html)/[Clerk](index.html)/[isInitialized](is-initialized.html)



# isInitialized



[androidJvm]\
val [isInitialized](is-initialized.html): StateFlow&lt;[Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)&gt;



Reactive state indicating whether the Clerk SDK has completed initialization.



Observe this StateFlow to know when the SDK is ready for authentication operations. The SDK must be initialized before calling authentication methods.




