package com.clerk.ui.organizationprofile.actions

import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.userdata.PublicUserData
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.delete
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkStatic
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationProfileActionConfirmationViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  @BeforeTest
  fun setUp() {
    mockkStatic("com.clerk.api.organizations.OrganizationKt")
    mockkStatic("com.clerk.api.organizations.OrganizationMembershipKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.organizations.OrganizationKt")
    unmockkStatic("com.clerk.api.organizations.OrganizationMembershipKt")
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
    coEvery { membership.delete() } returns
      ClerkResult.success(
        DeletedObject(objectType = "organization_membership", id = membership.id, deleted = true)
      )

    val viewModel = viewModel()
    viewModel.setConfirmationText("Acme Inc.")
    viewModel.confirm(
      action = OrganizationProfileConfirmationAction.LeaveOrganization,
      organization = organization,
      membership = membership,
    )

    assertTrue(viewModel.state.value.isLoading)
    advanceUntilIdle()

    coVerify(exactly = 1) { membership.delete() }
    assertTrue(viewModel.state.value.isComplete)
    assertFalse(viewModel.state.value.isLoading)
    assertEquals("Acme Inc.", viewModel.state.value.confirmationText)
    assertNull(viewModel.state.value.errorMessage)
  }

  @Test
  fun `confirm delete deletes organization and completes`() = runTest {
    val organization = organization()
    coEvery { organization.delete() } returns
      ClerkResult.success(
        DeletedObject(objectType = "organization", id = organization.id, deleted = true)
      )

    val viewModel = viewModel()
    viewModel.setConfirmationText("Acme Inc.")
    viewModel.confirm(
      action = OrganizationProfileConfirmationAction.DeleteOrganization,
      organization = organization,
      membership = membership(organization),
    )
    advanceUntilIdle()

    coVerify(exactly = 1) { organization.delete() }
    assertTrue(viewModel.state.value.isComplete)
    assertFalse(viewModel.state.value.isLoading)
    assertNull(viewModel.state.value.errorMessage)
  }

  @Test
  fun `confirm does nothing when organization name does not match`() = runTest {
    val organization = organization()
    coEvery { organization.delete() } returns
      ClerkResult.success(
        DeletedObject(objectType = "organization", id = organization.id, deleted = true)
      )

    val viewModel = viewModel()
    viewModel.setConfirmationText("Other Inc.")
    viewModel.confirm(
      action = OrganizationProfileConfirmationAction.DeleteOrganization,
      organization = organization,
      membership = membership(organization),
    )
    advanceUntilIdle()

    coVerify(exactly = 0) { organization.delete() }
    assertFalse(viewModel.state.value.isComplete)
    assertFalse(viewModel.state.value.isLoading)
    assertNull(viewModel.state.value.errorMessage)
  }

  @Test
  fun `failure keeps confirmation text and stores error`() = runTest {
    val organization = organization()
    coEvery { organization.delete() } returns
      ClerkResult.Failure(ClerkErrorResponse(errors = listOf(Error(longMessage = "boom"))))

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
    coEvery { organization.delete() } returns
      ClerkResult.Failure(ClerkErrorResponse(errors = listOf(Error(longMessage = "boom"))))

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
    return OrganizationProfileActionConfirmationViewModel(
      dispatcher = dispatcher,
      refreshClient = { ClerkResult.success(Client()) },
    )
  }

  private fun organization(): Organization {
    return Organization(
      id = "org_123",
      name = "Acme Inc.",
      slug = "acme",
      imageUrl = "",
      maxAllowedMemberships = 0,
      adminDeleteEnabled = true,
      createdAt = 1,
      updatedAt = 1,
      publicMetadata = JsonNull,
    )
  }

  private fun membership(organization: Organization): OrganizationMembership {
    return OrganizationMembership(
      id = "mem_123",
      publicMetadata = JsonNull,
      role = "org:admin",
      roleName = "Admin",
      permissions = emptyList(),
      publicUserData =
        PublicUserData(
          firstName = "Ada",
          lastName = "Lovelace",
          imageUrl = "",
          hasImage = false,
          identifier = "ada@example.com",
          userId = "user_123",
        ),
      organization = organization,
      createdAt = 1,
      updatedAt = 1,
    )
  }
}
