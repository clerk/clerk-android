package com.clerk.ui.organizationlist

import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.OrganizationCreationDefaults
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.organizations.PublicOrganizationData
import com.clerk.api.organizations.UserOrganizationInvitation
import com.clerk.api.organizations.accept
import com.clerk.api.session.Session
import com.clerk.api.user.User
import com.clerk.api.user.getOrganizationCreationDefaults
import com.clerk.api.user.getOrganizationInvitations
import com.clerk.api.user.getOrganizationMemberships
import com.clerk.api.user.getOrganizationSuggestions
import com.clerk.ui.organizationswitcher.previewOrganizationMembership
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import java.time.Instant
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

  @BeforeTest
  fun setUp() {
    mockkStatic("com.clerk.api.user.UserKt")
    mockkStatic("com.clerk.api.organizations.UserOrganizationInvitationKt")
    mockkStatic("com.clerk.api.organizations.OrganizationSuggestionKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkStatic("com.clerk.api.organizations.UserOrganizationInvitationKt")
    unmockkStatic("com.clerk.api.organizations.OrganizationSuggestionKt")
    unmockkAll()
  }

  @Test
  fun `load populates memberships invitations suggestions and creation defaults`() = runTest {
    val user = user(canCreateOrganization = true)
    val membership = membership("org_acme")
    val invitation = invitation("inv_1", organizationId = "org_invited")
    val suggestion = suggestion("sug_1", organizationId = "org_suggested")
    val defaults = OrganizationCreationDefaults(form = OrganizationCreationDefaults.Form("Acme"))
    coEvery { user.getOrganizationMemberships(limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(membership), totalCount = 1))
    coEvery { user.getOrganizationInvitations(limit = 2, offset = 0, status = "pending") } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(invitation), totalCount = 1))
    coEvery {
      user.getOrganizationSuggestions(
        limit = 2,
        offset = 0,
        statuses = listOf("pending", "accepted"),
      )
    } returns ClerkResult.success(ClerkPaginatedResponse(data = listOf(suggestion), totalCount = 1))
    coEvery { user.getOrganizationCreationDefaults() } returns ClerkResult.success(defaults)

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
    coEvery { user.getOrganizationMemberships(limit = 2, offset = 0) } returns
      ClerkResult.Failure(ClerkErrorResponse(errors = listOf(Error(longMessage = "boom"))))
    coEvery { user.getOrganizationInvitations(limit = 2, offset = 0, status = "pending") } returns
      ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))
    coEvery {
      user.getOrganizationSuggestions(
        limit = 2,
        offset = 0,
        statuses = listOf("pending", "accepted"),
      )
    } returns ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))

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
    coEvery { user.getOrganizationMemberships(limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(membership1), totalCount = 2))
    coEvery { user.getOrganizationMemberships(limit = 2, offset = 1) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(membership2), totalCount = 2))
    coEvery { user.getOrganizationInvitations(limit = 2, offset = 0, status = "pending") } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(invitation1), totalCount = 2))
    coEvery { user.getOrganizationInvitations(limit = 2, offset = 1, status = "pending") } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(invitation2), totalCount = 2))
    coEvery {
      user.getOrganizationSuggestions(
        limit = 2,
        offset = 0,
        statuses = listOf("pending", "accepted"),
      )
    } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(suggestion1), totalCount = 2))
    coEvery {
      user.getOrganizationSuggestions(
        limit = 2,
        offset = 1,
        statuses = listOf("pending", "accepted"),
      )
    } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(suggestion2), totalCount = 2))

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
    coEvery { user.getOrganizationMemberships(limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))
    coEvery { user.getOrganizationInvitations(limit = 2, offset = 0, status = "pending") } returns
      ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))
    coEvery {
      user.getOrganizationSuggestions(
        limit = 2,
        offset = 0,
        statuses = listOf("pending", "accepted"),
      )
    } returns ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))

    val viewModel = testViewModel(user = user)
    viewModel.load()

    assertTrue(viewModel.state.value.canShowNoOrganizationHelp)
  }

  @Test
  fun `acceptInvitation replaces invitation and marks organization selectable`() = runTest {
    val pending = invitation("inv_1", organizationId = "org_invited")
    val accepted = pending.copy(status = "accepted")
    coEvery { pending.accept() } returns ClerkResult.success(accepted)

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
  fun `acceptSuggestion replaces suggestion with accepted result`() = runTest {
    val pending = suggestion("sug_1", organizationId = "org_suggested", status = "pending")
    val accepted = pending.copy(status = "accepted")
    coEvery { pending.accept() } returns ClerkResult.success(accepted)

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

  private fun membership(organizationId: String) =
    previewOrganizationMembership(
      organizationId = organizationId,
      organizationName = organizationId,
    )

  private fun invitation(
    id: String,
    organizationId: String,
    status: String = "pending",
  ): UserOrganizationInvitation {
    return UserOrganizationInvitation(
      id = id,
      emailAddress = "user@example.com",
      publicOrganizationData =
        UserOrganizationInvitation.PublicOrganizationData(
          id = organizationId,
          name = organizationId,
          imageUrl = null,
          hasImage = false,
        ),
      publicMetadata = "{}",
      role = "org:member",
      status = status,
      createdAt = Instant.EPOCH,
      updatedAt = Instant.EPOCH,
    )
  }

  private fun suggestion(
    id: String,
    organizationId: String,
    status: String = "pending",
  ): OrganizationSuggestion {
    return OrganizationSuggestion(
      id = id,
      publicOrganizationData =
        PublicOrganizationData(
          id = organizationId,
          name = organizationId,
          imageUrl = null,
          hasImage = false,
          slug = null,
        ),
      status = status,
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private class TestOrganizationAccountListViewModel(
    private val testUser: User?,
    private val testSession: Session?,
    private val fetchDefaults: Boolean,
    dispatcher: CoroutineDispatcher,
  ) : OrganizationAccountListViewModel(pageSize = 2, workDispatcher = dispatcher) {
    fun setState(state: OrganizationAccountListState) {
      mutableState.value = state
    }

    override fun currentUser(): User? = testUser

    override fun currentSession(): Session? = testSession

    override fun shouldFetchCreationDefaults(user: User): Boolean = fetchDefaults
  }
}
