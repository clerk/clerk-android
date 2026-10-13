package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganizationGetInvitationsTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `getInvitations with the Unknown status fails locally without calling Clerk`() = runTest {
    mockkObject(ClerkApi)

    val result = organization().getInvitations(status = OrganizationInvitation.Status.Unknown)

    assertTrue(result is ClerkResult.Failure)
    assertEquals(
      "invitation_status_invalid",
      (result as ClerkResult.Failure).error?.errors?.single()?.code,
    )
    verify(exactly = 0) { ClerkApi.organization }
  }

  private fun organization() =
    Organization(
      id = "org_123",
      name = "Acme",
      slug = "acme",
      imageUrl = "https://img.clerk.com/acme",
      maxAllowedMemberships = 5,
      adminDeleteEnabled = true,
      createdAt = 1,
      updatedAt = 2,
      publicMetadata = JsonObject(emptyMap()),
    )
}
