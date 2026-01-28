package com.clerk.ui.userprofile.connectedaccount

import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.reauthorize
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.ResultType
import com.clerk.api.user.User
import com.clerk.api.user.createExternalAccount
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
class AddConnectedAccountViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.user } returns null
    every { Clerk.isGoogleOneTapEnabled } returns false
    mockkStatic("com.clerk.api.user.UserKt")
    mockkStatic("com.clerk.api.externalaccount.ExternalAccountKt")
    mockkObject(SignIn.Companion)
  }

  @After
  fun tearDown() {
    unmockkObject(SignIn.Companion)
    unmockkStatic("com.clerk.api.externalaccount.ExternalAccountKt")
    unmockkStatic("com.clerk.api.user.UserKt")
    unmockkAll()
  }

  @Test
  fun connectExternalAccount_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    val account = mockk<ExternalAccount>()
    every { Clerk.user } returns user
    coEvery { user.createExternalAccount(any()) } returns ClerkResult.success(account)
    coEvery { account.reauthorize() } returns ClerkResult.success(account)

    val viewModel = AddConnectedAccountViewModel()

    viewModel.connectExternalAccount(OAuthProvider.GITHUB)
    advanceUntilIdle()

    assertEquals(AddConnectedAccountViewModel.State.Success, viewModel.state.value)
  }

  @Test
  fun connectExternalAccount_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { Clerk.user } returns user
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "fail")))
    coEvery { user.createExternalAccount(any()) } returns ClerkResult.Failure(error)

    val viewModel = AddConnectedAccountViewModel()

    viewModel.connectExternalAccount(OAuthProvider.GITHUB)
    advanceUntilIdle()

    assertEquals(AddConnectedAccountViewModel.State.Error("fail"), viewModel.state.value)
  }

  @Test
  fun connectExternalAccount_withoutUser_setsGuardError() = runTest {
    every { Clerk.user } returns null

    val viewModel = AddConnectedAccountViewModel()

    viewModel.connectExternalAccount(OAuthProvider.GITHUB)
    advanceUntilIdle()

    assertEquals(
      AddConnectedAccountViewModel.State.Error("User does not exist"),
      viewModel.state.value,
    )
  }

  @Test
  fun googleOneTap_success_setsSuccessState() = runTest {
    val result = mockk<OAuthResult> { every { resultType } returns ResultType.SIGN_IN }
    every { Clerk.user } returns mockk()
    every { Clerk.isGoogleOneTapEnabled } returns true
    coEvery { SignIn.authenticateWithGoogleOneTap() } returns ClerkResult.success(result)

    val viewModel = AddConnectedAccountViewModel()

    viewModel.connectExternalAccount(OAuthProvider.GOOGLE)
    advanceUntilIdle()

    assertEquals(AddConnectedAccountViewModel.State.Success, viewModel.state.value)
  }

  @Test
  fun googleOneTap_unknownResult_setsErrorState() = runTest {
    val result = mockk<OAuthResult> { every { resultType } returns ResultType.UNKNOWN }
    every { Clerk.user } returns mockk()
    every { Clerk.isGoogleOneTapEnabled } returns true
    coEvery { SignIn.authenticateWithGoogleOneTap() } returns ClerkResult.success(result)

    val viewModel = AddConnectedAccountViewModel()

    viewModel.connectExternalAccount(OAuthProvider.GOOGLE)
    advanceUntilIdle()

    assertEquals(
      AddConnectedAccountViewModel.State.Error("Unknown result type"),
      viewModel.state.value,
    )
  }

  @Test
  fun googleOneTap_failure_setsErrorState() = runTest {
    val error = ClerkErrorResponse(errors = listOf(Error(longMessage = "nope")))
    every { Clerk.user } returns mockk()
    every { Clerk.isGoogleOneTapEnabled } returns true
    coEvery { SignIn.authenticateWithGoogleOneTap() } returns ClerkResult.Failure(error)

    val viewModel = AddConnectedAccountViewModel()

    viewModel.connectExternalAccount(OAuthProvider.GOOGLE)
    advanceUntilIdle()

    assertEquals(AddConnectedAccountViewModel.State.Error("nope"), viewModel.state.value)
  }
}
