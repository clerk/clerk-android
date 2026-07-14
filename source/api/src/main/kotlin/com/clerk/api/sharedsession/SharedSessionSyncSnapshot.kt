package com.clerk.api.sharedsession

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import java.security.MessageDigest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SharedSessionSyncSnapshot(
  val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
  val instanceId: String,
  val auth: AuthSnapshot? = null,
  val environment: EnvironmentSnapshot? = null,
  val deviceToken: DeviceTokenSnapshot? = null,
) {
  @Serializable
  data class AuthSnapshot(
    val state: State,
    val version: String,
    val serverFetchAtMillis: Long? = null,
    val client: Client? = null,
  )

  @Serializable
  data class EnvironmentSnapshot(
    val version: String,
    val changedAtMillis: Long,
    val value: Environment,
  )

  @Serializable
  data class DeviceTokenSnapshot(
    val state: State,
    val version: String,
    val changedAtMillis: Long,
    val value: String? = null,
  )

  @Serializable
  enum class State {
    @SerialName("set") SET,
    @SerialName("cleared") CLEARED,
  }

  companion object {
    const val CURRENT_SCHEMA_VERSION = 1

    fun instanceId(publishableKey: String): String {
      val digest =
        MessageDigest.getInstance("SHA-256").digest(publishableKey.toByteArray(Charsets.UTF_8))
      return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun encode(snapshot: SharedSessionSyncSnapshot): String =
      ClerkApi.json.encodeToString(serializer(), snapshot)

    fun decode(value: String): SharedSessionSyncSnapshot =
      ClerkApi.json.decodeFromString(serializer(), value)
  }
}

internal enum class SharedSessionSyncClientDecision {
  APPLY,
  IGNORE,
  REJECT_STALE,
}

@Suppress("CyclomaticComplexMethod", "ReturnCount")
internal fun decideSharedClientSnapshot(
  incoming: SharedSessionSyncSnapshot.AuthSnapshot,
  current: SharedSessionSyncSnapshot.AuthSnapshot?,
): SharedSessionSyncClientDecision {
  if (current == null) {
    return SharedSessionSyncClientDecision.APPLY
  }

  if (incoming.state == SharedSessionSyncSnapshot.State.CLEARED) {
    return decideSharedClientClear(incoming = incoming, current = current)
  }

  val incomingClient = incoming.client ?: return SharedSessionSyncClientDecision.IGNORE
  val currentClient = current.client
  val incomingFetchAt = incoming.serverFetchAtMillis
  val currentFetchAt = current.serverFetchAtMillis

  if (incomingFetchAt != null && currentFetchAt != null) {
    if (incomingFetchAt > currentFetchAt) return SharedSessionSyncClientDecision.APPLY
    if (incomingFetchAt < currentFetchAt) return SharedSessionSyncClientDecision.REJECT_STALE
    if (current.state == SharedSessionSyncSnapshot.State.CLEARED || currentClient == null) {
      return SharedSessionSyncClientDecision.REJECT_STALE
    }
    return decideEqualDateClients(incomingClient = incomingClient, currentClient = currentClient)
  }

  if (current.state == SharedSessionSyncSnapshot.State.CLEARED || currentClient == null) {
    return if (currentFetchAt == null || incomingFetchAt != null) {
      SharedSessionSyncClientDecision.APPLY
    } else {
      SharedSessionSyncClientDecision.REJECT_STALE
    }
  }

  if (incomingFetchAt != null) return SharedSessionSyncClientDecision.APPLY
  if (currentFetchAt != null) return SharedSessionSyncClientDecision.REJECT_STALE

  return decideEqualDateClients(incomingClient = incomingClient, currentClient = currentClient)
}

private fun decideSharedClientClear(
  incoming: SharedSessionSyncSnapshot.AuthSnapshot,
  current: SharedSessionSyncSnapshot.AuthSnapshot,
): SharedSessionSyncClientDecision {
  val incomingFetchAt = incoming.serverFetchAtMillis
  val currentFetchAt = current.serverFetchAtMillis

  if (incomingFetchAt == null || currentFetchAt == null) {
    return if (
      current.state != SharedSessionSyncSnapshot.State.CLEARED || currentFetchAt != incomingFetchAt
    ) {
      SharedSessionSyncClientDecision.APPLY
    } else {
      SharedSessionSyncClientDecision.IGNORE
    }
  }

  return when {
    incomingFetchAt > currentFetchAt -> SharedSessionSyncClientDecision.APPLY
    incomingFetchAt < currentFetchAt -> SharedSessionSyncClientDecision.REJECT_STALE
    current.state != SharedSessionSyncSnapshot.State.CLEARED ->
      SharedSessionSyncClientDecision.APPLY
    else -> SharedSessionSyncClientDecision.IGNORE
  }
}

private fun decideEqualDateClients(
  incomingClient: Client,
  currentClient: Client,
): SharedSessionSyncClientDecision {
  val incomingUpdatedAt = incomingClient.updatedAt
  val currentUpdatedAt = currentClient.updatedAt
  if (
    incomingUpdatedAt != null && currentUpdatedAt != null && incomingUpdatedAt > currentUpdatedAt
  ) {
    return SharedSessionSyncClientDecision.APPLY
  }
  return if (incomingClient == currentClient) {
    SharedSessionSyncClientDecision.IGNORE
  } else {
    SharedSessionSyncClientDecision.REJECT_STALE
  }
}
