---
title: HeaderMiddleware
---
//[Clerk Android](../../../index.html)/[com.clerk.network.middleware.outgoing](../index.html)/[HeaderMiddleware](index.html)



# HeaderMiddleware



[androidJvm]\
class [HeaderMiddleware](index.html) : Interceptor

HeaderMiddleware is an OkHttp interceptor that adds custom headers to outgoing requests. It adds the following headers:



- 
   X-Clerk-Client: &quot;android&quot;
- 
   clerk-api-version: &quot;{Current API Version}&quot;
- 
   x-android-sdk-version: &quot;{Current SDK Version}&quot;
- 
   x-mobile: &quot;1&quot;



## Constructors


| | |
|---|---|
| [HeaderMiddleware](-header-middleware.html) | [androidJvm]<br>constructor() |


## Functions


| Name | Summary |
|---|---|
| [intercept](intercept.html) | [androidJvm]<br>open override fun [intercept](intercept.html)(chain: Interceptor.Chain): Response |

