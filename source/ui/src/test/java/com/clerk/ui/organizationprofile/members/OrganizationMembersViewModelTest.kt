package com.clerk.ui.organizationprofile.members

import com.clerk.api.*
import com.clerk.testing.mockClerk
import com.clerk.testing.testCoreError
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.unmockkAll
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

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationMembersViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  @BeforeTest fun setUp() {}

  @AfterTest
  fun tearDown() {
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
    coEvery { organization.getRoles() } returns
      GetRolesResponse(data = roles, totalCount = 2.0, hasRoleSetMigration = true)

    coEvery {
      organization.getMemberships(GetMembersParams(query = null, pageSize = 2.0, initialPage = 1.0))
    } returns ClerkPaginatedResponseOrganizationMembership(data = listOf(member), totalCount = 1.0)

    coEvery {
      organization.getInvitations(
        GetInvitationsParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status = listOf(OrganizationInvitationStatus.Pending),
        )
      )
    } returns
      ClerkPaginatedResponseOrganizationInvitation(data = listOf(invitation), totalCount = 1.0)
    coEvery {
      organization.getMembershipRequests(
        GetMembershipRequestParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status = OrganizationInvitationStatus.Pending,
        )
      )
    } returns
      ClerkPaginatedResponseOrganizationMembershipRequest(data = listOf(request), totalCount = 1.0)

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
    assertTrue(state.hasRoleSetMigration)
    assertFalse(state.isLoadingInitial)
    assertNull(state.errorMessage)
  }

  @Test
  fun `load selects requested initial tab when available`() = runTest {
    val organization = organization()
    val viewer = viewerMembership()
    stubManageResources(organization)
    coEvery {
      organization.getMemberships(GetMembersParams(query = null, pageSize = 2.0, initialPage = 1.0))
    } returns ClerkPaginatedResponseOrganizationMembership(data = emptyList(), totalCount = 0.0)

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(
      organization = organization,
      membership = viewer,
      domainsEnabled = true,
      initialTab = OrganizationMembersTab.Invitations,
    )
    advanceUntilIdle()

    assertEquals(OrganizationMembersTab.Invitations, viewModel.state.value.selectedTab)
  }

  @Test
  fun `loadMoreMembers appends next page`() = runTest {
    val organization = organization()
    val viewer = viewerMembership()
    val first = member("mem_1", "Ada", "Lovelace")
    val second = member("mem_2", "Grace", "Hopper")
    stubManageResources(organization)
    coEvery {
      organization.getMemberships(GetMembersParams(query = null, pageSize = 1.0, initialPage = 1.0))
    } returns ClerkPaginatedResponseOrganizationMembership(data = listOf(first), totalCount = 2.0)
    coEvery {
      organization.getMemberships(GetMembersParams(query = null, pageSize = 1.0, initialPage = 2.0))
    } returns ClerkPaginatedResponseOrganizationMembership(data = listOf(second), totalCount = 2.0)

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
    val viewer = viewerMembership(permissions = listOf("org:sys_memberships:read"))
    coEvery {
      organization.getMemberships(GetMembersParams(query = null, pageSize = 2.0, initialPage = 1.0))
    } returns ClerkPaginatedResponseOrganizationMembership(data = emptyList(), totalCount = 0.0)
    coEvery {
      organization.getMemberships(
        GetMembersParams(query = "ada", pageSize = 2.0, initialPage = 1.0)
      )
    } returns
      ClerkPaginatedResponseOrganizationMembership(
        data = listOf(member("mem_1", "Ada")),
        totalCount = 1.0,
      )

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = false)
    advanceUntilIdle()

    viewModel.setMemberQuery("a")
    viewModel.setMemberQuery("ad")
    viewModel.setMemberQuery("ada")
    assertTrue(viewModel.state.value.isSearchingMembers)
    advanceTimeBy(299)
    coVerify(exactly = 0) {
      organization.getMemberships(
        GetMembersParams(query = "ada", pageSize = 2.0, initialPage = 1.0)
      )
    }
    advanceTimeBy(1)
    advanceUntilIdle()

    coVerify(exactly = 1) {
      organization.getMemberships(
        GetMembersParams(query = "ada", pageSize = 2.0, initialPage = 1.0)
      )
    }
    assertEquals("ada", viewModel.state.value.memberQuery)
    assertEquals("Ada", viewModel.state.value.members.first().publicUserData?.firstName)
    assertFalse(viewModel.state.value.isSearchingMembers)
  }

  @Test
  fun `submitMemberSearch runs current query immediately and cancels debounce`() = runTest {
    val organization = organization()
    val viewer = viewerMembership(permissions = listOf("org:sys_memberships:read"))
    coEvery {
      organization.getMemberships(GetMembersParams(query = null, pageSize = 2.0, initialPage = 1.0))
    } returns ClerkPaginatedResponseOrganizationMembership(data = emptyList(), totalCount = 0.0)
    coEvery {
      organization.getMemberships(
        GetMembersParams(query = "ada", pageSize = 2.0, initialPage = 1.0)
      )
    } returns
      ClerkPaginatedResponseOrganizationMembership(
        data = listOf(member("mem_1", "Ada")),
        totalCount = 1.0,
      )

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = false)
    advanceUntilIdle()

    viewModel.setMemberQuery("ada")
    viewModel.submitMemberSearch()
    assertTrue(viewModel.state.value.isSearchingMembers)
    advanceUntilIdle()
    advanceTimeBy(300)
    advanceUntilIdle()

    coVerify(exactly = 1) {
      organization.getMemberships(
        GetMembersParams(query = "ada", pageSize = 2.0, initialPage = 1.0)
      )
    }
    assertEquals("Ada", viewModel.state.value.members.first().publicUserData?.firstName)
    assertFalse(viewModel.state.value.isSearchingMembers)
  }

  @Test
  fun `role migration disables member role update`() = runTest {
    val organization = organization()
    val viewer = viewerMembership()
    val member = member("mem_1", "Ada")
    stubManageResources(organization)
    coEvery {
      organization.getMemberships(GetMembersParams(query = null, pageSize = 2.0, initialPage = 1.0))
    } returns ClerkPaginatedResponseOrganizationMembership(data = listOf(member), totalCount = 1.0)

    coEvery { organization.getRoles() } returns
      GetRolesResponse(hasRoleSetMigration = true, data = emptyList(), totalCount = 0.0)
    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = true)
    advanceUntilIdle()
    viewModel.updateMemberRole(member, "org:admin")
    advanceUntilIdle()

    coVerify(exactly = 0) { member.update(UpdateOrganizationMembershipParams(role = "org:admin")) }
    assertEquals(listOf(member), viewModel.state.value.members)
  }

  @Test
  fun `removeMember exposes active mutation and removes the member`() = runTest {
    val organization = organization()
    val viewer = viewerMembership()
    val member = member("mem_1", "Ada")
    stubManageResources(organization)
    coEvery {
      organization.getMemberships(GetMembersParams(query = null, pageSize = 2.0, initialPage = 1.0))
    } returns ClerkPaginatedResponseOrganizationMembership(data = listOf(member), totalCount = 1.0)
    coEvery { organization.removeMember(userId = "user_mem_1") } returns member

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
  fun `acceptRequest removes request from state`() = runTest {
    val organization = organization()
    val viewer = viewerMembership()
    val request = request("req_1")
    stubManageResources(organization)
    coEvery {
      organization.getMemberships(GetMembersParams(query = null, pageSize = 2.0, initialPage = 1.0))
    } returns ClerkPaginatedResponseOrganizationMembership(data = emptyList(), totalCount = 0.0)
    coEvery {
      organization.getMembershipRequests(
        GetMembershipRequestParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status = OrganizationInvitationStatus.Pending,
        )
      )
    } returns
      ClerkPaginatedResponseOrganizationMembershipRequest(data = listOf(request), totalCount = 1.0)
    coEvery { request.accept() } returns request

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
    val viewer = viewerMembership(permissions = listOf("org:sys_memberships:read"))
    coEvery {
      organization.getMemberships(GetMembersParams(query = null, pageSize = 2.0, initialPage = 1.0))
    } throws testCoreError("boom")

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewer, domainsEnabled = false)
    advanceUntilIdle()

    assertEquals("boom", viewModel.state.value.errorMessage)
    assertFalse(viewModel.state.value.isLoadingInitial)
  }

  private fun viewModel(pageSize: Int): OrganizationMembersViewModel {
    return OrganizationMembersViewModel(mockClerk(), pageSize = pageSize, dispatcher = dispatcher)
  }

  private fun stubManageResources(organization: Organization) {
    coEvery { organization.getRoles() } returns
      GetRolesResponse(
        data = listOf(role("org:admin", "Admin"), role("org:member", "Member")),
        totalCount = 2.0,
      )

    coEvery {
      organization.getInvitations(any())
    } returns ClerkPaginatedResponseOrganizationInvitation(data = emptyList(), totalCount = 0.0)
    coEvery {
      organization.getMembershipRequests(any())
    } returns
      ClerkPaginatedResponseOrganizationMembershipRequest(data = emptyList(), totalCount = 0.0)
  }

  private fun organization(): Organization =
    mockk(relaxed = true) {
      every { id } returns "org_123"
      every { name } returns "Acme"
    }

  private fun viewerMembership(
    permissions: List<String> = listOf("org:sys_memberships:read", "org:sys_memberships:manage")
  ): OrganizationMembership {
    val viewer = member("viewer", "Viewer")
    every { viewer.permissions } returns permissions
    return viewer
  }

  private fun member(
    id: String,
    firstName: String,
    lastName: String? = null,
    role: String = "org:member",
  ): OrganizationMembership {
    val member = mockk<OrganizationMembership>(relaxed = true)
    every { member.id } returns id
    every { member.role } returns role
    every { member.permissions } returns emptyList()
    val user = mockk<PublicUserData>(relaxed = true)
    every { user.userId } returns "user_$id"
    every { user.firstName } returns firstName
    every { user.lastName } returns lastName
    every { member.publicUserData } returns user
    return member
  }

  private fun invitation(id: String): OrganizationInvitation {
    val invitation = mockk<OrganizationInvitation>(relaxed = true)
    every { invitation.id } returns id
    return invitation
  }

  private fun request(id: String): OrganizationMembershipRequest {
    val request = mockk<OrganizationMembershipRequest>(relaxed = true)
    every { request.id } returns id
    return request
  }

  private fun role(key: String, name: String): Role {
    val role = mockk<Role>(relaxed = true)
    every { role.key } returns key
    every { role.name } returns name
    return role
  }
}
