package com.clerk.api.network.model.response

import com.clerk.api.network.model.client.Client
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ClientPiggybackedResponse<T>(
  @SerialName("response") val response: T,
  val client: Client? = null,
)
