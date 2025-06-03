---
title: intercept
---
//[Clerk Android](../../../index.html)/[com.clerk.network.middleware.incoming](../index.html)/[ClientSyncingMiddleware](index.html)/[intercept](intercept.html)



# intercept



[androidJvm]\
open override fun [intercept](intercept.html)(chain: Interceptor.Chain): Response



Intercepts network responses to sync client state.



#### Return



The original response, potentially with a new body if it was read for client syncing.



#### Parameters


androidJvm

| | |
|---|---|
| chain | The interceptor chain. |




