---
title: ClientSyncingMiddleware
---
//[Clerk Android](../../../index.html)/[com.clerk.network.middleware.incoming](../index.html)/[ClientSyncingMiddleware](index.html)



# ClientSyncingMiddleware



[androidJvm]\
class [ClientSyncingMiddleware](index.html)(json: Json) : Interceptor

Network middleware that automatically syncs the Clerk client state from API responses.



This middleware intercepts successful JSON responses and checks for a &quot;client&quot; field in the response body. If found, it deserializes the client data and updates the global [Clerk.client](../../com.clerk/-clerk/client.html) state.



## Constructors


| | |
|---|---|
| [ClientSyncingMiddleware](-client-syncing-middleware.html) | [androidJvm]<br>constructor(json: Json) |


## Functions


| Name | Summary |
|---|---|
| [intercept](intercept.html) | [androidJvm]<br>open override fun [intercept](intercept.html)(chain: Interceptor.Chain): Response<br>Intercepts network responses to sync client state. |

