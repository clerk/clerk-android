package com.clerk.ui.organizationprofile.invite

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.Role
import com.clerk.api.organizations.bulkCreateInvitations
import com.clerk.api.organizations.getRoles
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationInviteMembersViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.organizationDefaultRoleKey } returns "org:member"
    mockkStatic("com.clerk.api.organizations.OrganizationKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.organizations.OrganizationKt")
    unmockkAll()
  }

  @Test
  fun `loadRoles selects environment default role when present`() = runTest {
    val organization = mockk<Organization>()
    val roles = listOf(role("org:admin", "Admin"), role("org:member", "Member"))
    coEvery { organization.getRoles() } returns ClerkResult.success(roles)

    val viewModel = OrganizationInviteMembersViewModel()
    viewModel.state.test {
      assertEquals(OrganizationInviteMembersState(), awaitItem())
      viewModel.loadRoles(organization)
      assertEquals(OrganizationInviteMembersState(isLoadingRoles = true), awaitItem())
      assertEquals(
        OrganizationInviteMembersState(
          isLoadingRoles = false,
          roles = roles,
          selectedRoleKey = "org:member",
        ),
        awaitItem(),
      )
    }
  }

  @Test
  fun `sendInvitations submits selected role and marks completion`() = runTest {
    val organization = mockk<Organization>()
    val roles = listOf(role("org:member", "Member"))
    coEvery { organization.getRoles() } returns ClerkResult.success(roles)
    coEvery {
      organization.bulkCreateInvitations(
        emailAddresses = listOf("one@example.com", "two@example.com"),
        role = "org:member",
      )
    } returns ClerkResult.success(emptyList())

    val viewModel = OrganizationInviteMembersViewModel()
    viewModel.state.test {
      assertEquals(OrganizationInviteMembersState(), awaitItem())
      viewModel.loadRoles(organization)
      assertEquals(OrganizationInviteMembersState(isLoadingRoles = true), awaitItem())
      assertEquals(
        OrganizationInviteMembersState(roles = roles, selectedRoleKey = "org:member"),
        awaitItem(),
      )
      viewModel.sendInvitations(
        organization = organization,
        emailAddresses = listOf("one@example.com", "two@example.com"),
      )
      assertEquals(
        OrganizationInviteMembersState(
          roles = roles,
          selectedRoleKey = "org:member",
          isSubmitting = true,
        ),
        awaitItem(),
      )
      assertEquals(
        OrganizationInviteMembersState(
          roles = roles,
          selectedRoleKey = "org:member",
          completion = OrganizationInviteCompletion.Sent,
        ),
        awaitItem(),
      )
    }
  }

  private fun role(key: String, name: String): Role {
    return Role(
      id = key,
      key = key,
      name = name,
      description = name,
      permissions = emptyList(),
      createdAt = 0,
      updatedAt = 0,
    )
  }
}
