package com.clerk.ui.organizationprofile.update

import app.cash.turbine.test
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.deleteLogo
import com.clerk.api.organizations.reload
import com.clerk.api.organizations.update
import com.clerk.api.organizations.updateLogo
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationProfileUpdateViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkStatic("com.clerk.api.organizations.OrganizationKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.organizations.OrganizationKt")
    unmockkAll()
  }

  @Test
  fun `save success updates profile and reloads organization`() = runTest {
    val organization = organization()
    val updatedOrganization = organization("updated")
    val reloadedOrganization = organization("reloaded")
    coEvery { organization.update(name = "Acme Labs", slug = "acme-labs") } returns
      ClerkResult.success(updatedOrganization)
    coEvery { updatedOrganization.reload() } returns ClerkResult.success(reloadedOrganization)

    val viewModel = OrganizationProfileUpdateViewModel()
    viewModel.state.test {
      assertEquals(OrganizationProfileUpdateViewModel.State.Idle, awaitItem())
      viewModel.save(
        organization = organization,
        name = "Acme Labs",
        slug = "acme-labs",
        logoFile = null,
        removeLogo = false,
      )
      assertEquals(OrganizationProfileUpdateViewModel.State.Loading, awaitItem())
      assertEquals(
        OrganizationProfileUpdateViewModel.State.Success(reloadedOrganization),
        awaitItem(),
      )
    }
  }

  @Test
  fun `save uploads selected logo before reload`() = runTest {
    val organization = organization()
    val updatedOrganization = organization("updated")
    val logoOrganization = organization("logo")
    val reloadedOrganization = organization("reloaded")
    val logoFile = mockk<File>()
    coEvery { organization.update(name = "Acme Labs", slug = "acme-labs") } returns
      ClerkResult.success(updatedOrganization)
    coEvery { updatedOrganization.updateLogo(logoFile) } returns
      ClerkResult.success(logoOrganization)
    coEvery { logoOrganization.reload() } returns ClerkResult.success(reloadedOrganization)

    val viewModel = OrganizationProfileUpdateViewModel()
    viewModel.state.test {
      assertEquals(OrganizationProfileUpdateViewModel.State.Idle, awaitItem())
      viewModel.save(
        organization = organization,
        name = "Acme Labs",
        slug = "acme-labs",
        logoFile = logoFile,
        removeLogo = false,
      )
      assertEquals(OrganizationProfileUpdateViewModel.State.Loading, awaitItem())
      assertEquals(
        OrganizationProfileUpdateViewModel.State.Success(reloadedOrganization),
        awaitItem(),
      )
    }

    coVerify(exactly = 1) { updatedOrganization.updateLogo(logoFile) }
  }

  @Test
  fun `save removes logo before reload`() = runTest {
    val organization = organization()
    val updatedOrganization = organization("updated")
    val logoOrganization = organization("logo")
    val reloadedOrganization = organization("reloaded")
    coEvery { organization.update(name = "Acme Labs", slug = null) } returns
      ClerkResult.success(updatedOrganization)
    coEvery { updatedOrganization.deleteLogo() } returns ClerkResult.success(logoOrganization)
    coEvery { logoOrganization.reload() } returns ClerkResult.success(reloadedOrganization)

    val viewModel = OrganizationProfileUpdateViewModel()
    viewModel.state.test {
      assertEquals(OrganizationProfileUpdateViewModel.State.Idle, awaitItem())
      viewModel.save(
        organization = organization,
        name = "Acme Labs",
        slug = null,
        logoFile = null,
        removeLogo = true,
      )
      assertEquals(OrganizationProfileUpdateViewModel.State.Loading, awaitItem())
      assertEquals(
        OrganizationProfileUpdateViewModel.State.Success(reloadedOrganization),
        awaitItem(),
      )
    }

    coVerify(exactly = 1) { updatedOrganization.deleteLogo() }
  }

  @Test
  fun `save failure emits error state`() = runTest {
    val organization = organization()
    coEvery { organization.update(name = "Acme Labs", slug = "acme-labs") } returns
      ClerkResult.Failure(ClerkErrorResponse(errors = listOf(Error(longMessage = "boom"))))

    val viewModel = OrganizationProfileUpdateViewModel()
    viewModel.state.test {
      assertEquals(OrganizationProfileUpdateViewModel.State.Idle, awaitItem())
      viewModel.save(
        organization = organization,
        name = "Acme Labs",
        slug = "acme-labs",
        logoFile = null,
        removeLogo = false,
      )
      assertEquals(OrganizationProfileUpdateViewModel.State.Loading, awaitItem())
      assertEquals(
        OrganizationProfileUpdateViewModel.State.Error("Failed to update organization: boom"),
        awaitItem(),
      )
    }
  }

  private fun organization(id: String = "org_123"): Organization {
    return mockk(name = id)
  }
}
