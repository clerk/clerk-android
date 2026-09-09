package com.clerk.ui.organizationlist

import com.clerk.api.*
import com.clerk.testing.mockClerk
import com.clerk.testing.testCoreError
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationAccountListViewModelTest {

  private val dispatcher = UnconfinedTestDispatcher()
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  @BeforeTest fun setUp() {}

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `load populates memberships invitations suggestions and creation defaults`() = runTest {
    val user = user(canCreateOrganization = true)
    val membership = membership("org_acme")
    val invitation = invitation("inv_1", organizationId = "org_invited")
    val suggestion = suggestion("sug_1", organizationId = "org_suggested")
    val defaults = mockk<OrganizationCreationDefaults>(relaxed = true)
    coEvery {
      user.getOrganizationMemberships(
        GetUserOrganizationMembershipParams(pageSize = 2.0, initialPage = 1.0)
      )
    } returns
      ClerkPaginatedResponseOrganizationMembership(data = listOf(membership), totalCount = 1.0)
    coEvery {
      user.getOrganizationInvitations(
        GetUserOrganizationInvitationsParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status = OrganizationInvitationStatus.Pending,
        )
      )
    } returns
      ClerkPaginatedResponseUserOrganizationInvitation(data = listOf(invitation), totalCount = 1.0)
    coEvery {
      user.getOrganizationSuggestions(
        GetUserOrganizationSuggestionsParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status =
            GetUserOrganizationSuggestionsParamsStatus.Case3(
              listOf(OrganizationSuggestionStatus.Pending, OrganizationSuggestionStatus.Accepted)
            ),
        )
      )
    } returns
      ClerkPaginatedResponseOrganizationSuggestion(data = listOf(suggestion), totalCount = 1.0)
    coEvery { user.getOrganizationCreationDefaults() } returns defaults

    val viewModel = testViewModel(user = user, fetchDefaults = true)
    viewModel.load()

    val state = viewModel.state.value
    assertFalse(state.isLoading)
    assertTrue(state.hasLoadedInitialResources)
    assertEquals(listOf(membership), state.memberships)
    assertEquals(listOf(invitation), state.invitations)
    assertEquals(listOf(suggestion), state.suggestions)
    assertEquals(defaults, state.creationDefaults)
    assertNull(state.errorMessage)
  }

  @Test
  fun `load failure marks initial load as failed`() = runTest {
    val user = user()
    coEvery {
      user.getOrganizationMemberships(
        GetUserOrganizationMembershipParams(pageSize = 2.0, initialPage = 1.0)
      )
    } throws testCoreError("boom")
    coEvery {
      user.getOrganizationInvitations(
        GetUserOrganizationInvitationsParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status = OrganizationInvitationStatus.Pending,
        )
      )
    } returns ClerkPaginatedResponseUserOrganizationInvitation(data = emptyList(), totalCount = 0.0)
    coEvery {
      user.getOrganizationSuggestions(
        GetUserOrganizationSuggestionsParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status =
            GetUserOrganizationSuggestionsParamsStatus.Case3(
              listOf(OrganizationSuggestionStatus.Pending, OrganizationSuggestionStatus.Accepted)
            ),
        )
      )
    } returns ClerkPaginatedResponseOrganizationSuggestion(data = emptyList(), totalCount = 0.0)

    val viewModel = testViewModel(user = user)
    viewModel.load()

    val state = viewModel.state.value
    assertTrue(state.initialLoadFailed)
    assertEquals("boom", state.errorMessage)
  }

  @Test
  fun `loadMore appends each paginated resource type`() = runTest {
    val user = user()
    val membership1 = membership("org_1")
    val membership2 = membership("org_2")
    val invitation1 = invitation("inv_1", organizationId = "org_inv_1")
    val invitation2 = invitation("inv_2", organizationId = "org_inv_2")
    val suggestion1 = suggestion("sug_1", organizationId = "org_sug_1")
    val suggestion2 = suggestion("sug_2", organizationId = "org_sug_2")
    coEvery {
      user.getOrganizationMemberships(
        GetUserOrganizationMembershipParams(pageSize = 2.0, initialPage = 1.0)
      )
    } returns
      ClerkPaginatedResponseOrganizationMembership(data = listOf(membership1), totalCount = 2.0)
    coEvery {
      user.getOrganizationMemberships(
        GetUserOrganizationMembershipParams(pageSize = 2.0, initialPage = 1.5)
      )
    } returns
      ClerkPaginatedResponseOrganizationMembership(data = listOf(membership2), totalCount = 2.0)
    coEvery {
      user.getOrganizationInvitations(
        GetUserOrganizationInvitationsParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status = OrganizationInvitationStatus.Pending,
        )
      )
    } returns
      ClerkPaginatedResponseUserOrganizationInvitation(data = listOf(invitation1), totalCount = 2.0)
    coEvery {
      user.getOrganizationInvitations(
        GetUserOrganizationInvitationsParams(
          pageSize = 2.0,
          initialPage = 1.5,
          status = OrganizationInvitationStatus.Pending,
        )
      )
    } returns
      ClerkPaginatedResponseUserOrganizationInvitation(data = listOf(invitation2), totalCount = 2.0)
    coEvery {
      user.getOrganizationSuggestions(
        GetUserOrganizationSuggestionsParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status =
            GetUserOrganizationSuggestionsParamsStatus.Case3(
              listOf(OrganizationSuggestionStatus.Pending, OrganizationSuggestionStatus.Accepted)
            ),
        )
      )
    } returns
      ClerkPaginatedResponseOrganizationSuggestion(data = listOf(suggestion1), totalCount = 2.0)
    coEvery {
      user.getOrganizationSuggestions(
        GetUserOrganizationSuggestionsParams(
          pageSize = 2.0,
          initialPage = 1.5,
          status =
            GetUserOrganizationSuggestionsParamsStatus.Case3(
              listOf(OrganizationSuggestionStatus.Pending, OrganizationSuggestionStatus.Accepted)
            ),
        )
      )
    } returns
      ClerkPaginatedResponseOrganizationSuggestion(data = listOf(suggestion2), totalCount = 2.0)

    val viewModel = testViewModel(user = user)
    viewModel.load()
    viewModel.loadMoreMemberships()
    viewModel.loadMoreInvitations()
    viewModel.loadMoreSuggestions()

    val state = viewModel.state.value
    assertEquals(listOf(membership1, membership2), state.memberships)
    assertEquals(listOf(invitation1, invitation2), state.invitations)
    assertEquals(listOf(suggestion1, suggestion2), state.suggestions)
    assertFalse(state.hasNextPage)
  }

  @Test
  fun `empty loaded resources without create enabled can show empty help`() = runTest {
    val user = user(canCreateOrganization = false)
    coEvery {
      user.getOrganizationMemberships(
        GetUserOrganizationMembershipParams(pageSize = 2.0, initialPage = 1.0)
      )
    } returns ClerkPaginatedResponseOrganizationMembership(data = emptyList(), totalCount = 0.0)
    coEvery {
      user.getOrganizationInvitations(
        GetUserOrganizationInvitationsParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status = OrganizationInvitationStatus.Pending,
        )
      )
    } returns ClerkPaginatedResponseUserOrganizationInvitation(data = emptyList(), totalCount = 0.0)
    coEvery {
      user.getOrganizationSuggestions(
        GetUserOrganizationSuggestionsParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status =
            GetUserOrganizationSuggestionsParamsStatus.Case3(
              listOf(OrganizationSuggestionStatus.Pending, OrganizationSuggestionStatus.Accepted)
            ),
        )
      )
    } returns ClerkPaginatedResponseOrganizationSuggestion(data = emptyList(), totalCount = 0.0)

    val viewModel = testViewModel(user = user)
    viewModel.load()

    assertTrue(viewModel.state.value.canShowNoOrganizationHelp)
  }

  @Test
  fun `acceptInvitation replaces invitation and marks organization selectable`() = runTest {
    val pending = invitation("inv_1", organizationId = "org_invited")
    val accepted = invitation("inv_1", organizationId = "org_invited", status = "accepted")
    coEvery { pending.accept() } returns accepted

    val viewModel = testViewModel()
    viewModel.setState(
      OrganizationAccountListState(
        isLoading = false,
        hasLoadedInitialResources = true,
        invitations = listOf(pending),
        invitationsTotalCount = 1,
      )
    )
    viewModel.acceptInvitation(pending)

    val state = viewModel.state.value
    assertEquals(listOf(accepted), state.invitations)
    assertEquals(0, state.invitationsTotalCount)
    assertTrue("org_invited" in state.acceptedInvitationOrganizationIds)
    assertNull(state.activeActionId)
  }

  @Test
  fun `accepted invitation does not count toward next pending invitation offset`() = runTest {
    val user = user()
    val pending = invitation("inv_1", organizationId = "org_invited")
    val accepted = invitation("inv_1", organizationId = "org_invited", status = "accepted")
    val nextPending = invitation("inv_2", organizationId = "org_next")
    coEvery { pending.accept() } returns accepted
    coEvery {
      user.getOrganizationInvitations(
        GetUserOrganizationInvitationsParams(
          pageSize = 2.0,
          initialPage = 1.0,
          status = OrganizationInvitationStatus.Pending,
        )
      )
    } returns
      ClerkPaginatedResponseUserOrganizationInvitation(data = listOf(nextPending), totalCount = 1.0)

    val viewModel = testViewModel(user = user)
    viewModel.setState(
      OrganizationAccountListState(
        isLoading = false,
        hasLoadedInitialResources = true,
        invitations = listOf(pending),
        invitationsTotalCount = 2,
      )
    )

    viewModel.acceptInvitation(pending)
    assertTrue(viewModel.state.value.invitationsHasNextPage)

    viewModel.loadMoreInvitations()

    val state = viewModel.state.value
    assertEquals(listOf(accepted, nextPending), state.invitations)
    assertFalse(state.invitationsHasNextPage)
  }

  @Test
  fun `acceptSuggestion replaces suggestion with accepted result`() = runTest {
    val pending = suggestion("sug_1", organizationId = "org_suggested", status = "pending")
    val accepted = suggestion("sug_1", organizationId = "org_suggested", status = "accepted")
    coEvery { pending.accept() } returns accepted

    val viewModel = testViewModel()
    viewModel.setState(
      OrganizationAccountListState(
        isLoading = false,
        hasLoadedInitialResources = true,
        suggestions = listOf(pending),
        suggestionsTotalCount = 1,
      )
    )
    viewModel.acceptSuggestion(pending)

    val state = viewModel.state.value
    assertEquals(listOf(accepted), state.suggestions)
    assertNull(state.activeActionId)
  }

  private fun testViewModel(
    user: User? = null,
    session: Session? = null,
    fetchDefaults: Boolean = false,
  ): TestOrganizationAccountListViewModel {
    return TestOrganizationAccountListViewModel(
      testUser = user,
      testSession = session,
      fetchDefaults = fetchDefaults,
      dispatcher = dispatcher,
    )
  }

  private fun user(canCreateOrganization: Boolean = true): User {
    return mockk { every { createOrganizationEnabled } returns canCreateOrganization }
  }

  private fun membership(organizationId: String): OrganizationMembership {
    val membership = mockk<OrganizationMembership>(relaxed = true)
    every { membership.id } returns "mem_$organizationId"
    every { membership.organization.id } returns organizationId
    return membership
  }

  private fun invitation(
    id: String,
    organizationId: String,
    status: String = "pending",
  ): UserOrganizationInvitation {
    val invitation = mockk<UserOrganizationInvitation>(relaxed = true)
    every { invitation.id } returns id
    every { invitation.publicOrganizationData.id } returns organizationId
    every { invitation.status } returns
      if (status == "accepted") OrganizationInvitationStatus.Accepted
      else OrganizationInvitationStatus.Pending
    return invitation
  }

  private fun suggestion(
    id: String,
    organizationId: String,
    status: String = "pending",
  ): OrganizationSuggestion {
    val suggestion = mockk<OrganizationSuggestion>(relaxed = true)
    every { suggestion.id } returns id
    every { suggestion.publicOrganizationData.id } returns organizationId
    every { suggestion.status } returns
      if (status == "accepted") OrganizationSuggestionStatus.Accepted
      else OrganizationSuggestionStatus.Pending
    return suggestion
  }

  private class TestOrganizationAccountListViewModel(
    private val testUser: User?,
    private val testSession: Session?,
    private val fetchDefaults: Boolean,
    dispatcher: CoroutineDispatcher,
  ) :
    OrganizationAccountListViewModel(
      mockClerk(testUser),
      pageSize = 2,
      workDispatcher = dispatcher,
    ) {
    fun setState(state: OrganizationAccountListState) {
      mutableState.value = state
    }

    override fun currentUser(): User? = testUser

    override fun currentSession(): Session? = testSession

    override fun shouldFetchCreationDefaults(user: User): Boolean = fetchDefaults
  }
}
