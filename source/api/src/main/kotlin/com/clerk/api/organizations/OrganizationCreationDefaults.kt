package com.clerk.api.organizations

import kotlinx.serialization.Serializable

@Serializable
public data class OrganizationCreationDefaults(
  val advisory: Advisory? = null,
  val form: Form? = null,
) {
  @Serializable
  public data class Advisory(
    val code: String,
    val severity: String? = null,
    val meta: Map<String, String> = emptyMap(),
  )

  @Serializable
  public data class Form(
    val name: String,
    val slug: String? = null,
    val logo: String? = null,
    val blurHash: String? = null,
  )
}
