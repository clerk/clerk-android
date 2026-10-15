package com.clerk.ui.organizationprofile.create

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class OrganizationCreateFlowViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    mockkObject(Organization.Companion)
    every { Clerk.session } returns mockk { every { id } returns "sess_1" }
  }

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `a taken slug becomes the slug field error`() = runTest {
    coEvery { Organization.create(name = "Acme", slug = "acme") } returns
      ClerkResult.Failure(
        ClerkErrorResponse(
          errors =
            listOf(
              Error(
                message = "That slug is taken. Please try another.",
                longMessage = "That slug is taken. Please try another.",
                code = "form_identifier_exists",
                meta = buildJsonObject { put("param_name", JsonPrimitive("slug")) },
              )
            )
        )
      )

    val viewModel = OrganizationCreateFlowViewModel()
    viewModel.state.test {
      awaitItem()
      viewModel.createOrganization(name = "Acme", slug = "acme", logoFile = null)
      assertEquals(OrganizationCreateFlowState(isLoading = true), awaitItem())
      assertEquals(
        OrganizationCreateFlowState(slugError = "That slug is taken. Please try another."),
        awaitItem(),
      )
      viewModel.clearSlugError()
      assertEquals(OrganizationCreateFlowState(), awaitItem())
    }
  }

  @Test
  fun `an error about another field stays a general error`() = runTest {
    coEvery { Organization.create(name = "Acme", slug = "acme") } returns
      ClerkResult.Failure(
        ClerkErrorResponse(
          errors =
            listOf(
              Error(
                longMessage = "Name is too long.",
                code = "form_param_format_invalid",
                meta = buildJsonObject { put("param_name", JsonPrimitive("name")) },
              )
            )
        )
      )

    val viewModel = OrganizationCreateFlowViewModel()
    viewModel.state.test {
      awaitItem()
      viewModel.createOrganization(name = "Acme", slug = "acme", logoFile = null)
      assertEquals(OrganizationCreateFlowState(isLoading = true), awaitItem())
      assertEquals(OrganizationCreateFlowState(errorMessage = "Name is too long."), awaitItem())
    }
  }
}
