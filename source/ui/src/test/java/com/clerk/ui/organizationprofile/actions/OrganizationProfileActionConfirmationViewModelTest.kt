package com.clerk.ui.organizationprofile.actions

import com.clerk.api.*
import com.clerk.testing.mockClerk
import com.clerk.testing.testCoreError
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.*
import io.mockk.coEvery
import io.mockk.coVerify
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationProfileActionConfirmationViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  @BeforeTest fun setUp() {}

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `confirmation matching trims whitespace and normalizes smart quotes`() {
    assertTrue(organizationNameConfirmationMatches(" Acme Inc. ", "Acme Inc."))
    assertTrue(organizationNameConfirmationMatches("Acme \u201CInc.\u201D", "Acme \"Inc.\""))
    assertFalse(organizationNameConfirmationMatches("acme inc.", "Acme Inc."))
  }

  @Test
  fun `confirm leave deletes membership and completes`() = runTest {
    val organization = organization()
    val membership = membership(organization)
    coEvery { membership.destroy() } returns mockk(relaxed = true)

    val viewModel = viewModel()
    viewModel.setConfirmationText("Acme Inc.")
    viewModel.confirm(
      action = OrganizationProfileConfirmationAction.LeaveOrganization,
      organization = organization,
      membership = membership,
    )

    assertTrue(viewModel.state.value.isLoading)
    advanceUntilIdle()

    coVerify(exactly = 1) { membership.destroy() }
    assertTrue(viewModel.state.value.isComplete)
    assertFalse(viewModel.state.value.isLoading)
    assertEquals("Acme Inc.", viewModel.state.value.confirmationText)
    assertNull(viewModel.state.value.errorMessage)
  }

  @Test
  fun `confirm delete deletes organization and completes`() = runTest {
    val organization = organization()
    coEvery { organization.destroy() } returns mockk(relaxed = true)

    val viewModel = viewModel()
    viewModel.setConfirmationText("Acme Inc.")
    viewModel.confirm(
      action = OrganizationProfileConfirmationAction.DeleteOrganization,
      organization = organization,
      membership = membership(organization),
    )
    advanceUntilIdle()

    coVerify(exactly = 1) { organization.destroy() }
    assertTrue(viewModel.state.value.isComplete)
    assertFalse(viewModel.state.value.isLoading)
    assertNull(viewModel.state.value.errorMessage)
  }

  @Test
  fun `confirm does nothing when organization name does not match`() = runTest {
    val organization = organization()
    coEvery { organization.destroy() } returns mockk(relaxed = true)

    val viewModel = viewModel()
    viewModel.setConfirmationText("Other Inc.")
    viewModel.confirm(
      action = OrganizationProfileConfirmationAction.DeleteOrganization,
      organization = organization,
      membership = membership(organization),
    )
    advanceUntilIdle()

    coVerify(exactly = 0) { organization.destroy() }
    assertFalse(viewModel.state.value.isComplete)
    assertFalse(viewModel.state.value.isLoading)
    assertNull(viewModel.state.value.errorMessage)
  }

  @Test
  fun `failure keeps confirmation text and stores error`() = runTest {
    val organization = organization()
    coEvery { organization.destroy() } throws testCoreError("boom")

    val viewModel = viewModel()
    viewModel.setConfirmationText("Acme Inc.")
    viewModel.confirm(
      action = OrganizationProfileConfirmationAction.DeleteOrganization,
      organization = organization,
      membership = membership(organization),
    )
    advanceUntilIdle()

    assertEquals("Acme Inc.", viewModel.state.value.confirmationText)
    assertEquals("boom", viewModel.state.value.errorMessage)
    assertFalse(viewModel.state.value.isLoading)
    assertFalse(viewModel.state.value.isComplete)
  }

  @Test
  fun `clearError removes stored error without resetting form`() = runTest {
    val organization = organization()
    coEvery { organization.destroy() } throws testCoreError("boom")

    val viewModel = viewModel()
    viewModel.setConfirmationText("Acme Inc.")
    viewModel.confirm(
      action = OrganizationProfileConfirmationAction.DeleteOrganization,
      organization = organization,
      membership = membership(organization),
    )
    advanceUntilIdle()
    viewModel.clearError()

    assertEquals("Acme Inc.", viewModel.state.value.confirmationText)
    assertNull(viewModel.state.value.errorMessage)
  }

  @Test
  fun `leave without membership remains open and shows error`() = runTest {
    val viewModel = viewModel()
    viewModel.setConfirmationText("Acme Inc.")
    viewModel.confirm(
      action = OrganizationProfileConfirmationAction.LeaveOrganization,
      organization = organization(),
      membership = null,
    )

    assertFalse(viewModel.state.value.isComplete)
    assertFalse(viewModel.state.value.isLoading)
    assertEquals(
      "Unable to leave organization because no active membership was found.",
      viewModel.state.value.errorMessage,
    )
  }

  private fun viewModel(): OrganizationProfileActionConfirmationViewModel {
    return OrganizationProfileActionConfirmationViewModel(clerk = mockClerk())
  }

  private fun organization(): Organization =
    mockk(relaxed = true) {
      every { id } returns "org_123"
      every { name } returns "Acme Inc."
    }

  private fun membership(organization: Organization): OrganizationMembership =
    mockk(relaxed = true) {
      every { id } returns "mem_123"
      every { this@mockk.organization } returns organization
    }
}
