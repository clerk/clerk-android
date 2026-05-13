package com.clerk.ui.organizationswitcher

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.session.Session
import com.clerk.api.user.User
import com.clerk.api.user.getOrganizationMemberships
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationSwitcherViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    mockkStatic("com.clerk.api.user.UserKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkAll()
  }

  @Test
  fun load_withNullUser_resetsState() {
    val viewModel = OrganizationSwitcherViewModel()
    viewModel.load(null)

    val state = viewModel.state.value
    assertEquals(OrganizationSwitcherState(), state)
  }

  @Test
  fun load_success_populatesMembershipsAndCount() = runTest {
    val user = mockk<User>()
    val membership = membership(organizationId = "org_acme", name = "Acme")
    coEvery { user.getOrganizationMemberships(limit = any(), offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(membership), totalCount = 1))

    val viewModel = OrganizationSwitcherViewModel()
    viewModel.load(user)

    val state = viewModel.state.value
    assertEquals(listOf(membership), state.memberships)
    assertEquals(1, state.membershipsTotalCount)
    assertTrue(state.initialLoadAttempted)
    assertEquals(false, state.isLoading)
    assertNull(state.errorMessage)
  }

  @Test
  fun load_failure_emitsErrorAndStopsLoading() = runTest {
    val user = mockk<User>()
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "boom")))
    coEvery { user.getOrganizationMemberships(limit = any(), offset = 0) } returns
      ClerkResult.Failure(error)

    val viewModel = OrganizationSwitcherViewModel()
    viewModel.load(user)

    val state = viewModel.state.value
    assertEquals(emptyList(), state.memberships)
    assertEquals(false, state.isLoading)
    assertEquals("boom", state.errorMessage)
  }

  @Test
  fun load_calledTwice_doesNotRefetchAfterInitialAttempt() = runTest {
    val user = mockk<User>()
    coEvery { user.getOrganizationMemberships(limit = any(), offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))

    val viewModel = OrganizationSwitcherViewModel()
    viewModel.load(user)
    viewModel.load(user)

    coVerify(exactly = 1) { user.getOrganizationMemberships(limit = any(), offset = 0) }
  }

  @Test
  fun loadMore_appendsNextPageAndUpdatesTotalCount() = runTest {
    val user = mockk<User>()
    val first = membership(organizationId = "org_acme", name = "Acme")
    val second = membership(organizationId = "org_beta", name = "Beta")
    coEvery { user.getOrganizationMemberships(limit = any(), offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(first), totalCount = 2))
    coEvery { user.getOrganizationMemberships(limit = any(), offset = 1) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(second), totalCount = 2))

    val viewModel = OrganizationSwitcherViewModel()
    viewModel.load(user)
    viewModel.loadMore(user)

    val state = viewModel.state.value
    assertEquals(listOf(first, second), state.memberships)
    assertEquals(2, state.membershipsTotalCount)
    assertEquals(false, state.hasNextPage)
  }

  @Test
  fun loadMore_skipsWhenNoNextPage() = runTest {
    val user = mockk<User>()
    coEvery { user.getOrganizationMemberships(limit = any(), offset = 0) } returns
      ClerkResult.success(
        ClerkPaginatedResponse(
          data = listOf(membership(organizationId = "org_acme", name = "Acme")),
          totalCount = 1,
        )
      )

    val viewModel = OrganizationSwitcherViewModel()
    viewModel.load(user)
    viewModel.loadMore(user)

    coVerify(exactly = 0) { user.getOrganizationMemberships(limit = any(), offset = 1) }
  }

  @Test
  fun loadMore_failure_emitsErrorAndKeepsExistingMemberships() = runTest {
    val user = mockk<User>()
    val first = membership(organizationId = "org_acme", name = "Acme")
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "boom")))
    coEvery { user.getOrganizationMemberships(limit = any(), offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(first), totalCount = 2))
    coEvery { user.getOrganizationMemberships(limit = any(), offset = 1) } returns
      ClerkResult.Failure(error)

    val viewModel = OrganizationSwitcherViewModel()
    viewModel.load(user)
    viewModel.loadMore(user)

    val state = viewModel.state.value
    assertEquals(listOf(first), state.memberships)
    assertEquals("boom", state.errorMessage)
    assertEquals(false, state.isLoadingMore)
  }

  @Test
  fun selectOrganization_success_clearsActiveActionAndCallsBack() = runTest {
    val session = mockk<Session>()
    every { session.id } returns "sess_123"
    coEvery { Clerk.auth.setActive(sessionId = "sess_123", organizationId = "org_acme") } returns
      ClerkResult.success(session)

    val viewModel = OrganizationSwitcherViewModel()
    var invoked = false
    viewModel.selectOrganization(session = session, organizationId = "org_acme") { invoked = true }

    val state = viewModel.state.value
    assertNull(state.activeActionId)
    assertNull(state.errorMessage)
    assertTrue(invoked)
    coVerify { Clerk.auth.setActive(sessionId = "sess_123", organizationId = "org_acme") }
  }

  @Test
  fun selectOrganization_nullSession_emitsErrorAndDoesNotCallSetActive() = runTest {
    coEvery { Clerk.auth.setActive(any(), any()) } returns ClerkResult.success(mockk())

    val viewModel = OrganizationSwitcherViewModel()
    var invoked = false
    viewModel.selectOrganization(session = null, organizationId = "org_acme") { invoked = true }

    val state = viewModel.state.value
    assertEquals("Session does not exist", state.errorMessage)
    assertEquals(false, invoked)
    coVerify(exactly = 0) { Clerk.auth.setActive(any(), any()) }
  }

  @Test
  fun selectOrganization_failure_emitsErrorAndClearsActiveAction() = runTest {
    val session = mockk<Session>()
    every { session.id } returns "sess_123"
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "denied")))
    coEvery { Clerk.auth.setActive(sessionId = "sess_123", organizationId = "org_acme") } returns
      ClerkResult.Failure(error)

    val viewModel = OrganizationSwitcherViewModel()
    var invoked = false
    viewModel.selectOrganization(session = session, organizationId = "org_acme") { invoked = true }

    val state = viewModel.state.value
    assertNull(state.activeActionId)
    assertEquals("denied", state.errorMessage)
    assertEquals(false, invoked)
  }

  @Test
  fun selectOrganization_concurrentCallsAreDeduped() = runTest {
    val session = mockk<Session>()
    every { session.id } returns "sess_123"
    // Force a long-running first call so the second call sees activeActionId set
    coEvery { Clerk.auth.setActive(any(), any()) } coAnswers
      {
        kotlinx.coroutines.delay(1_000)
        ClerkResult.success(session)
      }

    val viewModel = OrganizationSwitcherViewModel()
    viewModel.selectOrganization(session = session, organizationId = "org_acme") {}
    viewModel.selectOrganization(session = session, organizationId = "org_beta") {}

    coVerify(exactly = 1) { Clerk.auth.setActive(any(), any()) }
  }

  @Test
  fun reset_clearsAllState() = runTest {
    val user = mockk<User>()
    coEvery { user.getOrganizationMemberships(limit = any(), offset = 0) } returns
      ClerkResult.success(
        ClerkPaginatedResponse(
          data = listOf(membership(organizationId = "org_acme", name = "Acme")),
          totalCount = 1,
        )
      )

    val viewModel = OrganizationSwitcherViewModel()
    viewModel.load(user)
    viewModel.reset()

    assertEquals(OrganizationSwitcherState(), viewModel.state.value)
  }

  @Test
  fun clearError_removesErrorMessage() = runTest {
    val user = mockk<User>()
    coEvery { user.getOrganizationMemberships(limit = any(), offset = 0) } returns
      ClerkResult.Failure(ClerkErrorResponse(errors = listOf(Error(longMessage = "boom"))))

    val viewModel = OrganizationSwitcherViewModel()
    viewModel.load(user)
    assertEquals("boom", viewModel.state.value.errorMessage)

    viewModel.clearError()
    assertNull(viewModel.state.value.errorMessage)
  }

  private fun membership(organizationId: String, name: String): OrganizationMembership {
    return previewOrganizationMembership(organizationId = organizationId, organizationName = name)
  }
}
