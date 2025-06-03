---
title: ClientPiggybackedResponse
---
//[Clerk Android](../../../index.html)/[com.clerk.model.response](../index.html)/[ClientPiggybackedResponse](index.html)



# ClientPiggybackedResponse



[androidJvm]\
@Serializable



data class [ClientPiggybackedResponse](index.html)&lt;[T](index.html)&gt;(val response: [T](index.html), val client: [Client](../../com.clerk.model.client/-client/index.html)? = null)

Represents a response from the Clerk API that includes a client object.



Clerk has a concept of &quot;piggybacking&quot; a client object on top of the response. This means that the response can contain additional information about the client, such as its ID, sessions, and authentication status.



## Constructors


| | |
|---|---|
| [ClientPiggybackedResponse](-client-piggybacked-response.html) | [androidJvm]<br>constructor(response: [T](index.html), client: [Client](../../com.clerk.model.client/-client/index.html)? = null) |


## Properties


| Name | Summary |
|---|---|
| [client](client.html) | [androidJvm]<br>val [client](client.html): [Client](../../com.clerk.model.client/-client/index.html)? = null<br>The client object associated with the response, if available. |
| [response](response.html) | [androidJvm]<br>@SerialName(value = &quot;response&quot;)<br>val [response](response.html): [T](index.html)<br>The actual response data from the Clerk API. It will be of type T, which can be any type that is serializable. |

