---
title: DeviceTokenSavingMiddleware
---
//[Clerk Android](../../../index.html)/[com.clerk.network.middleware.incoming](../index.html)/[DeviceTokenSavingMiddleware](index.html)



# DeviceTokenSavingMiddleware



[androidJvm]\
class [DeviceTokenSavingMiddleware](index.html) : Interceptor

OkHttp interceptor that saves device tokens from response headers to local storage.



This middleware intercepts network responses and checks for the presence of an Authorization header. If found, the token is automatically saved to the device's local storage for future use.



## Constructors


| | |
|---|---|
| [DeviceTokenSavingMiddleware](-device-token-saving-middleware.html) | [androidJvm]<br>constructor() |


## Functions


| Name | Summary |
|---|---|
| [intercept](intercept.html) | [androidJvm]<br>open override fun [intercept](intercept.html)(chain: Interceptor.Chain): Response<br>Intercepts the network response to save any device token present in the Authorization header. |

