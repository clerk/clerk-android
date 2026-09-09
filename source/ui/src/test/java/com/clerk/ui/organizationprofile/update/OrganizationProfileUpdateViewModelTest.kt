package com.clerk.ui.organizationprofile.update

import app.cash.turbine.test
import com.clerk.api.*
import com.clerk.testing.mockClerk
import com.clerk.testing.testCoreError
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationProfileUpdateViewModelTest {

  @get:org.junit.Rule val temporaryFolder = org.junit.rules.TemporaryFolder()

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest fun setUp() {}

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `save success updates profile and reloads organization`() = runTest {
    val organization = organization()
    val updatedOrganization = organization("updated")
    val reloadedOrganization = organization("reloaded")
    coEvery { organization.update(UpdateOrganizationParams("Acme Labs", "acme-labs")) } returns
      updatedOrganization
    coEvery { organization.reload() } returns reloadedOrganization

    val viewModel = OrganizationProfileUpdateViewModel(mockClerk())
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
        OrganizationProfileUpdateViewModel.State.Success(organization),
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
    val logoFile = temporaryFolder.newFile("logo.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    coEvery { organization.update(UpdateOrganizationParams("Acme Labs", "acme-labs")) } returns
      updatedOrganization
    coEvery { organization.setLogo(any()) } returns logoOrganization
    coEvery { organization.reload() } returns reloadedOrganization

    val viewModel = OrganizationProfileUpdateViewModel(mockClerk())
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
        OrganizationProfileUpdateViewModel.State.Success(organization),
        awaitItem(),
      )
    }

    coVerify(exactly = 1) { organization.setLogo(any()) }
  }

  @Test
  fun `save removes logo before reload`() = runTest {
    val organization = organization()
    val updatedOrganization = organization("updated")
    val logoOrganization = organization("logo")
    val reloadedOrganization = organization("reloaded")
    coEvery { organization.update(UpdateOrganizationParams("Acme Labs", null)) } returns
      updatedOrganization
    coEvery { organization.setLogo(SetOrganizationLogoParams(null)) } returns logoOrganization
    coEvery { organization.reload() } returns reloadedOrganization

    val viewModel = OrganizationProfileUpdateViewModel(mockClerk())
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
        OrganizationProfileUpdateViewModel.State.Success(organization),
        awaitItem(),
      )
    }

    coVerify(exactly = 1) { organization.setLogo(SetOrganizationLogoParams(null)) }
  }

  @Test
  fun `save failure emits error state`() = runTest {
    val organization = organization()
    coEvery { organization.update(UpdateOrganizationParams("Acme Labs", "acme-labs")) } throws
      testCoreError("boom")

    val viewModel = OrganizationProfileUpdateViewModel(mockClerk())
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

  @Test
  fun `clearError resets error state to idle`() = runTest {
    val organization = organization()
    coEvery { organization.update(UpdateOrganizationParams("Acme Labs", "acme-labs")) } throws
      testCoreError("boom")

    val viewModel = OrganizationProfileUpdateViewModel(mockClerk())
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
      viewModel.clearError()
      assertEquals(OrganizationProfileUpdateViewModel.State.Idle, awaitItem())
    }
  }

  private fun organization(id: String = "org_123"): Organization {
    return mockk(name = id)
  }
}
