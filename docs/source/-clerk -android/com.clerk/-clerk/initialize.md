---
title: initialize
---
//[Clerk Android](../../../index.html)/[com.clerk](../index.html)/[Clerk](index.html)/[initialize](initialize.html)



# initialize



[androidJvm]\
fun [initialize](initialize.html)(context: [Context](https://developer.android.com/reference/kotlin/android/content/Context.html), publishableKey: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-string/index.html), debugMode: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) = false)



Initializes the Clerk SDK with the provided configuration.



This method must be called before using any other Clerk functionality. It configures the API client, initializes local storage, and begins the authentication state setup.



#### Parameters


androidJvm

| | |
|---|---|
| context | The application context used for initialization and storage setup. |
| publishableKey | The publishable key from your Clerk Dashboard that connects your app to Clerk. |
| debugMode | Enable additional logging and debugging information (default: false). |



#### Throws


| | |
|---|---|
| [IllegalArgumentException](https://developer.android.com/reference/kotlin/java/lang/IllegalArgumentException.html) | if the publishable key format is invalid. |



