package com.clerk.ui.sessiontask.organization

import com.clerk.api.OrganizationCreationDefaults
import kotlinx.serialization.Serializable

/**
 * Editable initial form values saved with navigation, without persisting a live resource handle.
 */
@Serializable
internal data class OrganizationCreationPrefill(
  val name: String,
  val slug: String,
  val advisory: ExistingOrganizationNotice? = null,
)

@Serializable internal data class ExistingOrganizationNotice(val name: String, val domain: String)

internal fun OrganizationCreationDefaults.prefill() =
  OrganizationCreationPrefill(
    name = form.name,
    slug = form.slug,
    advisory =
      advisory
        ?.takeIf { it.code == "organization_already_exists" }
        ?.let {
          ExistingOrganizationNotice(
            it.meta["organization_name"].orEmpty(),
            it.meta["organization_domain"].orEmpty(),
          )
        },
  )
