package com.clerk.api.configuration

import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

  fun restoreMissingState(): Boolean {
    val restoredClient =
      Clerk.restoreCachedClient(client = client, serverFetchAtMillis = clientServerFetchAtMillis)
    val restoredEnvironment = Clerk.restoreCachedEnvironment(environment)
    ClerkLog.d("Restored from cache (client: $restoredClient, environment: $restoredEnvironment)")
    return (restoredClient || restoredEnvironment) &&
      Clerk.clientInitialized &&
      Clerk.environment != null
  }

  companion object {
    fun loadIfNeeded(publishableKey: String, baseUrl: String): CachedClerkState? {
      val needed = Clerk.clientFlow.value == null || Clerk.environment == null
      val cachedJson = if (needed) StorageHelper.loadValue(StorageKey.CACHED_CLERK_STATE) else null
      val cachedState = cachedJson?.let {
        runCatching { ClerkApi.json.decodeFromString(serializer(), it) }
          .onFailure { error ->
            ClerkLog.w("Failed to decode cached Clerk state: ${error.message}")
          }
          .getOrNull()
      }
      val matches = cachedState?.matchesConfiguration(publishableKey, baseUrl) == true
      if (cachedState != null && !matches) {
        ClerkLog.d("Ignoring cached Clerk state for a different configuration")
      }
      return cachedState?.takeIf { matches }
    }
  }
}
