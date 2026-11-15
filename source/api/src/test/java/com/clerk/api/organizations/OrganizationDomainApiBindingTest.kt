package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.OrganizationApi
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrganizationDomainApiBindingTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `domain calls use the organization service of the current configuration`() = runTest {
    val firstService = mockk<OrganizationApi>()
    val secondService = mockk<OrganizationApi>()
    coEvery { firstService.deleteOrganizationDomain(any(), any(), any()) } returns
      ClerkResult.success(DeletedObject(id = "orgdmn_123", deleted = true))
    coEvery { secondService.deleteOrganizationDomain(any(), any(), any()) } returns
      ClerkResult.success(DeletedObject(id = "orgdmn_123", deleted = true))
    mockkObject(ClerkApi)
    every { ClerkApi.organization } returns firstService

    organizationDomain().delete()

    every { ClerkApi.organization } returns secondService

    organizationDomain().delete()

    coVerify(exactly = 1) { firstService.deleteOrganizationDomain("org_123", "orgdmn_123", any()) }
    coVerify(exactly = 1) { secondService.deleteOrganizationDomain("org_123", "orgdmn_123", any()) }
  }

  private fun organizationDomain(): OrganizationDomain =
    OrganizationDomain(
      id = "orgdmn_123",
      name = "example.com",
      organizationId = "org_123",
      enrollmentMode = "manual_invitation",
      totalPendingInvitations = 0,
      createdAt = 1_713_200_000_000,
      updatedAt = 1_713_200_000_000,
      totalPendingSuggestions = 0,
    )
}
