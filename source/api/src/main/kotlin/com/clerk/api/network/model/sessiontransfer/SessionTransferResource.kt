package com.clerk.api.network.model.sessiontransfer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SessionTransferResource(
  @SerialName("object") val objectType: String,
  val url: String,
  @SerialName("expires_at") val expiresAt: Long,
)
