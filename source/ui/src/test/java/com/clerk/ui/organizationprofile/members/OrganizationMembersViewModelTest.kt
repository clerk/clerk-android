package com.clerk.ui.organizationprofile.members

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.userdata.PublicUserData
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationMembershipRequest
import com.clerk.api.organizations.OrganizationSystemPermission
import com.clerk.api.organizations.Role
import com.clerk.api.organizations.accept
import com.clerk.api.organizations.bulkCreateInvitations
import com.clerk.api.organizations.getInvitations
import com.clerk.api.organizations.getMembershipRequests
import com.clerk.api.organizations.getOrganizationMemberships
import com.clerk.api.organizations.getRoles
import com.clerk.api.organizations.removeMember
import com.clerk.api.organizations.updateMembership
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationMembersViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.organizationDefaultRoleKey } returns "org:member"
    mockkStatic("com.clerk.api.organizations.OrganizationKt")
    mockkStatic("com.clerk.api.organizations.OrganizationMembershipKt")
    mockkStatic("com.clerk.api.organizations.OrganizationMembershipRequestKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.organizations.OrganizationKt")
    unmockkStatic("com.clerk.api.organizations.OrganizationMembershipKt")
    unmockkStatic("com.clerk.api.organizations.OrganizationMembershipRequestKt")
    unmockkAll()
  }

  @Test
  fun `load populates visible resources and role migration flag`() = runTest {
    val organization = organization()
    val viewer = viewerMembership()
    val member = member("mem_1", "Ada", "Lovelace")
    val invitation = invitation("inv_1")
    val request = request("req_1")
    val roles = listOf(role("org:admin", "Admin"), role("org:member", "Member"))
    coEvery { organization.getRoles() } returns ClerkResult.success(roles)
    coEvery { organization.getOrganizationMemberships(query = null, limit = 2, offset = 0) } returns
      ClerkResult.success(
        ClerkPaginatedResponse(data = listOf(member), totalCount = 1, hasRoleSetMigration = true)
      )
    coEvery {
      organization.getInvitations(
        limit = 2,
        offset = 0,
        status = OrganizationInvitation.Status.Pending,
      )
    } returns ClerkResult.success(ClerkPaginatedResponse(data = listOf(invitation), totalCount = 1))
    coEvery {
      organization.getMembershipRequests(limit = 2, offset = 0, status = "pending")
    } returns ClerkResult.success(ClerkPaginatedResponse(data = listOf(request), totalCount = 1))

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = true)
    advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals(
      listOf(
        OrganizationMembersTab.Members,
        OrganizationMembersTab.Invitations,
        OrganizationMembersTab.Requests,
      ),
      state.availableTabs,
    )
    assertEquals(OrganizationMembersTab.Members, state.selectedTab)
    assertEquals(listOf(member), state.members)
    assertEquals(listOf(invitation), state.invitations)
    assertEquals(listOf(request), state.requests)
    assertEquals("org:member", state.selectedInviteRoleKey)
    assertTrue(state.hasRoleSetMigration)
    assertFalse(state.isLoadingInitial)
    assertNull(state.errorMessage)
  }

  @Test
  fun `loadMoreMembers appends next page`() = runTest {
    val organization = organization()
    val viewer = viewerMembership()
    val first = member("mem_1", "Ada", "Lovelace")
    val second = member("mem_2", "Grace", "Hopper")
    stubManageResources(organization)
    coEvery { organization.getOrganizationMemberships(query = null, limit = 1, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(first), totalCount = 2))
    coEvery { organization.getOrganizationMemberships(query = null, limit = 1, offset = 1) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(second), totalCount = 2))

    val viewModel = viewModel(pageSize = 1)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = true)
    advanceUntilIdle()
    viewModel.loadMoreMembers()
    advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals(listOf(first, second), state.members)
    assertFalse(state.membersHasNextPage)
  }

  @Test
  fun `member search is debounced and skips duplicate rapid requests`() = runTest {
    val organization = organization()
    val viewer =
      viewerMembership(permissions = listOf(OrganizationSystemPermission.READ_MEMBERSHIPS))
    coEvery { organization.getOrganizationMemberships(query = null, limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))
    coEvery {
      organization.getOrganizationMemberships(query = "ada", limit = 2, offset = 0)
    } returns
      ClerkResult.success(
        ClerkPaginatedResponse(data = listOf(member("mem_1", "Ada")), totalCount = 1)
      )

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = false)
    advanceUntilIdle()

    viewModel.setMemberQuery("a")
    viewModel.setMemberQuery("ad")
    viewModel.setMemberQuery("ada")
    advanceTimeBy(299)
    coVerify(exactly = 0) {
      organization.getOrganizationMemberships(query = "ada", limit = 2, offset = 0)
    }
    advanceTimeBy(1)
    advanceUntilIdle()

    coVerify(exactly = 1) {
      organization.getOrganizationMemberships(query = "ada", limit = 2, offset = 0)
    }
    assertEquals("ada", viewModel.state.value.memberQuery)
    assertEquals("Ada", viewModel.state.value.members.first().publicUserData?.firstName)
  }

  @Test
  fun `role migration disables member role update`() = runTest {
    val organization = organization()
    val viewer = viewerMembership()
    val member = member("mem_1", "Ada")
    stubManageResources(organization)
    coEvery { organization.getOrganizationMemberships(query = null, limit = 2, offset = 0) } returns
      ClerkResult.success(
        ClerkPaginatedResponse(data = listOf(member), totalCount = 1, hasRoleSetMigration = true)
      )

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = true)
    advanceUntilIdle()
    viewModel.updateMemberRole(member, "org:admin")
    advanceUntilIdle()

    coVerify(exactly = 0) { member.updateMembership(userId = "user_mem_1", role = "org:admin") }
    assertEquals(listOf(member), viewModel.state.value.members)
  }

  @Test
  fun `removeMember exposes active mutation and removes the member`() = runTest {
    val organization = organization()
    val viewer = viewerMembership()
    val member = member("mem_1", "Ada")
    stubManageResources(organization)
    coEvery { organization.getOrganizationMemberships(query = null, limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(member), totalCount = 1))
    coEvery { organization.removeMember(userId = "user_mem_1") } returns ClerkResult.success(member)

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = true)
    advanceUntilIdle()
    viewModel.removeMember(member)

    assertEquals(member.id, viewModel.state.value.activeMutationId)
    advanceUntilIdle()
    assertEquals(emptyList(), viewModel.state.value.members)
    assertEquals(0, viewModel.state.value.membersTotalCount)
    assertNull(viewModel.state.value.activeMutationId)
  }

  @Test
  fun `sendInvitations blocks when invites would exceed membership limit`() = runTest {
    val organization = organization(maxAllowedMemberships = 2, membersCount = 1, pendingCount = 0)
    val viewer = viewerMembership()
    stubManageResources(organization)
    coEvery { organization.getOrganizationMemberships(query = null, limit = 2, offset = 0) } returns
      ClerkResult.success(
        ClerkPaginatedResponse(data = listOf(member("mem_1", "Ada")), totalCount = 1)
      )

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = true)
    advanceUntilIdle()
    viewModel.addInviteEmails("one@example.com two@example.com")
    viewModel.sendInvitations()
    advanceUntilIdle()

    coVerify(exactly = 0) { organization.bulkCreateInvitations(any<List<String>>(), any<String>()) }
    assertEquals(
      "Invite limit would exceed allowed memberships",
      viewModel.state.value.errorMessage,
    )
  }

  @Test
  fun `acceptRequest removes request from state`() = runTest {
    val organization = organization()
    val viewer = viewerMembership()
    val request = request("req_1")
    stubManageResources(organization)
    coEvery { organization.getOrganizationMemberships(query = null, limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))
    coEvery {
      organization.getMembershipRequests(limit = 2, offset = 0, status = "pending")
    } returns ClerkResult.success(ClerkPaginatedResponse(data = listOf(request), totalCount = 1))
    coEvery { request.accept() } returns ClerkResult.success(request.copy(status = "accepted"))

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = true)
    advanceUntilIdle()
    viewModel.acceptRequest(request)
    advanceUntilIdle()

    assertEquals(emptyList(), viewModel.state.value.requests)
    assertEquals(0, viewModel.state.value.requestsTotalCount)
  }

  @Test
  fun `load failure stores error message`() = runTest {
    val organization = organization()
    val viewer =
      viewerMembership(permissions = listOf(OrganizationSystemPermission.READ_MEMBERSHIPS))
    coEvery { organization.getOrganizationMemberships(query = null, limit = 2, offset = 0) } returns
      ClerkResult.Failure(ClerkErrorResponse(errors = listOf(Error(longMessage = "boom"))))

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = false)
    advanceUntilIdle()

    assertEquals("boom", viewModel.state.value.errorMessage)
    assertFalse(viewModel.state.value.isLoadingInitial)
  }

  private fun viewModel(pageSize: Int): OrganizationMembersViewModel {
    return OrganizationMembersViewModel(pageSize = pageSize, dispatcher = dispatcher)
  }

  private fun stubManageResources(organization: Organization) {
    coEvery { organization.getRoles() } returns
      ClerkResult.success(listOf(role("org:admin", "Admin"), role("org:member", "Member")))
    coEvery {
      organization.getInvitations(
        limit = any(),
        offset = any(),
        status = OrganizationInvitation.Status.Pending,
      )
    } returns ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))
    coEvery {
      organization.getMembershipRequests(limit = any(), offset = any(), status = "pending")
    } returns ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))
  }

  private fun organization(
    maxAllowedMemberships: Int = 0,
    membersCount: Int? = null,
    pendingCount: Int? = null,
  ): Organization {
    return Organization(
      id = "org_123",
      name = "Acme",
      slug = "acme",
      imageUrl = "",
      membersCount = membersCount,
      pendingInvitationsCount = pendingCount,
      maxAllowedMemberships = maxAllowedMemberships,
      adminDeleteEnabled = true,
      createdAt = 1,
      updatedAt = 1,
      publicMetadata = JsonNull,
    )
  }

  private fun viewerMembership(
    permissions: List<OrganizationSystemPermission> =
      listOf(
        OrganizationSystemPermission.READ_MEMBERSHIPS,
        OrganizationSystemPermission.MANAGE_MEMBERSHIPS,
      )
  ): OrganizationMembership {
    return member("viewer", "Viewer").copy(permissions = permissions.map { it.value })
  }

  private fun member(
    id: String,
    firstName: String,
    lastName: String? = null,
    role: String = "org:member",
  ): OrganizationMembership {
    return OrganizationMembership(
      id = id,
      publicMetadata = JsonNull,
      role = role,
      roleName = if (role == "org:admin") "Admin" else "Member",
      permissions = emptyList(),
      publicUserData =
        PublicUserData(
          firstName = firstName,
          lastName = lastName,
          imageUrl = "",
          hasImage = false,
          identifier = "${firstName.lowercase()}@example.com",
          userId = "user_$id",
        ),
      organization = organization(),
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun invitation(id: String): OrganizationInvitation {
    return OrganizationInvitation(
      id = id,
      emailAddress = "$id@example.com",
      organizationId = "org_123",
      publicMetadata = JsonNull,
      role = "org:member",
      status = OrganizationInvitation.Status.Pending,
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun request(id: String): OrganizationMembershipRequest {
    return OrganizationMembershipRequest(
      id = id,
      organizationId = "org_123",
      publicUserData =
        PublicUserData(
          firstName = "Request",
          lastName = id,
          imageUrl = "",
          hasImage = false,
          identifier = "$id@example.com",
          userId = "user_$id",
        ),
      status = "pending",
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun role(key: String, name: String): Role {
    return Role(
      id = key,
      key = key,
      name = name,
      description = name,
      permissions = emptyList(),
      createdAt = 1,
      updatedAt = 1,
    )
  }
}
