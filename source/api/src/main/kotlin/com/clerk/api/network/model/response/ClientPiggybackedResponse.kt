package com.clerk.api.network.model.response

import com.clerk.api.network.model.client.Client
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a response from the Clerk API that includes a client object.
 *
 * Clerk has a concept of "piggybacking" a client object on top of the response. This means that the
 * response can contain additional information about the client, such as its ID, sessions, and
 * authentication status.
 *
 * @property response The actual response data from the Clerk API. It will be of type T, which can
 *   be any type that is serializable.
 * @property client The client object associated with the response, if available.
 */
@Serializable
internal data class ClientPiggybackedResponse<T>(
  @SerialName("response") val response: T,
  val client: Client? = null,
)
