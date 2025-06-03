---
title: intercept
---
//[Clerk Android](../../../index.html)/[com.clerk.network.middleware.incoming](../index.html)/[DeviceTokenSavingMiddleware](index.html)/[intercept](intercept.html)



# intercept



[androidJvm]\
open override fun [intercept](intercept.html)(chain: Interceptor.Chain): Response



Intercepts the network response to save any device token present in the Authorization header.



#### Return



The unmodified response after saving any device token found.



#### Parameters


androidJvm

| | |
|---|---|
| chain | The interceptor chain containing the request and response information. |




