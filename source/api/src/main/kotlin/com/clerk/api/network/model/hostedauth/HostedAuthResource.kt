package com.clerk.api.network.model.hostedauth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class HostedAuthResource(
  @SerialName("object") val objectType: String,
  val url: String,
)
