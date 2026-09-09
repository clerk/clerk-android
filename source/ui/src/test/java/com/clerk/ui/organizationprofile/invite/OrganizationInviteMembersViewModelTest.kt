package com.clerk.ui.organizationprofile.invite

import app.cash.turbine.test
import com.clerk.api.*
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationInviteMembersViewModelTest {
  private val clerk = mockk<Clerk>(relaxed = true)

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    every { clerk.environment.organizationSettings.domains.defaultRole } returns "org:member"
  }

  @AfterTest
  fun tearDown() {

    unmockkAll()
  }

  @Test
  fun `loadRoles selects environment default role when present`() = runTest {
    val organization = mockk<Organization>()
    val roles = listOf(role("org:admin", "Admin"), role("org:member", "Member"))
    coEvery { organization.getRoles() } returns
      GetRolesResponse(data = roles, totalCount = roles.size.toDouble())

    val viewModel = OrganizationInviteMembersViewModel(clerk)
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
    coEvery { organization.getRoles() } returns
      GetRolesResponse(data = roles, totalCount = roles.size.toDouble())
    coEvery {
      organization.inviteMembers(
        InviteMembersParams(listOf("one@example.com", "two@example.com"), "org:member")
      )
    } returns emptyList()

    val viewModel = OrganizationInviteMembersViewModel(clerk)
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
    val role = mockk<Role>(relaxed = true)
    every { role.key } returns key
    every { role.name } returns name
    return role
  }
}
