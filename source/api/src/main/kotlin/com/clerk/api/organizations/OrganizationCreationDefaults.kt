package com.clerk.api.organizations

import kotlinx.serialization.Serializable

@Serializable
data class OrganizationCreationDefaults(val advisory: Advisory? = null, val form: Form? = null) {
  @Serializable
  data class Advisory(
    val code: String,
    val severity: String? = null,
    val meta: Map<String, String> = emptyMap(),
  )

  @Serializable
  data class Form(
    val name: String,
    val slug: String? = null,
    val logo: String? = null,
    val blurHash: String? = null,
  )
}
