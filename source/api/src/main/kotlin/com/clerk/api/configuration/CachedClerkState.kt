package com.clerk.api.configuration

import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Persisted SDK state used to provide a complete representation during offline cold starts. */
@Serializable
internal data class CachedClerkState(
  @SerialName("publishable_key") val publishableKey: String,
  @SerialName("base_url") val baseUrl: String,
  val client: Client,
  val environment: Environment,
  @SerialName("client_server_fetch_at_millis") val clientServerFetchAtMillis: Long,
) {
  fun matchesConfiguration(publishableKey: String, baseUrl: String): Boolean =
    this.publishableKey == publishableKey && this.baseUrl == baseUrl
}
