package com.clerk.api.network.model.environment

import kotlinx.serialization.Serializable

@Serializable
internal data class OrganizationSettings(
  val enabled: Boolean = false,
  val maxAllowedMemberships: Int = 1,
  val forceOrganizationSelection: Boolean = false,
  val actions: Actions = Actions(),
  val domains: Domains = Domains(),
  val slug: Slug = Slug(),
  val organizationCreationDefaults: CreationDefaults = CreationDefaults(),
) {
  @Serializable data class Actions(val adminDelete: Boolean = false)

  @Serializable
  data class Domains(
    val enabled: Boolean = false,
    val enrollmentModes: List<String> = emptyList(),
    val defaultRole: String? = null,
  )

  @Serializable data class Slug(val disabled: Boolean = false)

  @Serializable data class CreationDefaults(val enabled: Boolean = false)
}
