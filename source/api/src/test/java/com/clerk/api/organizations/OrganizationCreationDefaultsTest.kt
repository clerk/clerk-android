package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrganizationCreationDefaultsTest {

  @Test
  fun `decodes partial organization creation defaults`() {
    val defaults =
      ClerkApi.json.decodeFromString<OrganizationCreationDefaults>(
        """
        {
          "advisory": {
            "code": "organization_already_exists",
            "meta": {
              "organization_name": "Acme",
              "organization_domain": "acme.test"
            }
          },
          "form": {
            "name": "Acme",
            "logo": "https://img.clerk.com/acme.png"
          }
        }
        """
          .trimIndent()
      )

    assertEquals("organization_already_exists", defaults.advisory?.code)
    assertEquals("Acme", defaults.advisory?.meta?.get("organization_name"))
    assertEquals("Acme", defaults.form?.name)
    assertNull(defaults.form?.slug)
    assertEquals("https://img.clerk.com/acme.png", defaults.form?.logo)
  }
}
