package com.clerk.api.network.model.environment

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganizationSettingsSerializationTest {

  @Test
  fun `decodes missing organization settings fields with defaults`() {
    val settings = ClerkApi.json.decodeFromString<OrganizationSettings>("{}")

    assertFalse(settings.enabled)
    assertEquals(1, settings.maxAllowedMemberships)
    assertFalse(settings.forceOrganizationSelection)
    assertFalse(settings.actions.adminDelete)
    assertFalse(settings.domains.enabled)
    assertEquals(emptyList<String>(), settings.domains.enrollmentModes)
    assertFalse(settings.slug.disabled)
    assertFalse(settings.organizationCreationDefaults.enabled)
  }

  @Test
  fun `decodes null organization settings children with defaults`() {
    val settings =
      ClerkApi.json.decodeFromString<OrganizationSettings>(
        """
        {
          "actions": null,
          "domains": null,
          "slug": null,
          "organization_creation_defaults": null
        }
        """
          .trimIndent()
      )

    assertFalse(settings.actions.adminDelete)
    assertFalse(settings.domains.enabled)
    assertFalse(settings.slug.disabled)
    assertFalse(settings.organizationCreationDefaults.enabled)
  }

  @Test
  fun `decodes complete organization settings payload`() {
    val settings =
      ClerkApi.json.decodeFromString<OrganizationSettings>(
        """
        {
          "enabled": true,
          "max_allowed_memberships": 5,
          "force_organization_selection": true,
          "actions": {
            "admin_delete": true
          },
          "domains": {
            "enabled": true,
            "enrollment_modes": ["automatic_invitation"],
            "default_role": "org:member"
          },
          "slug": {
            "disabled": true
          },
          "organization_creation_defaults": {
            "enabled": true
          }
        }
        """
          .trimIndent()
      )

    assertTrue(settings.enabled)
    assertEquals(5, settings.maxAllowedMemberships)
    assertTrue(settings.forceOrganizationSelection)
    assertTrue(settings.actions.adminDelete)
    assertTrue(settings.domains.enabled)
    assertEquals(listOf("automatic_invitation"), settings.domains.enrollmentModes)
    assertEquals("org:member", settings.domains.defaultRole)
    assertTrue(settings.slug.disabled)
    assertTrue(settings.organizationCreationDefaults.enabled)
  }
}
