package com.clerk.ui.userprofile.connectedaccount

import com.clerk.api.*
import com.clerk.testing.testCoreError
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
class AddConnectedAccountViewModelTest {
  private val clerk = mockk<Clerk>(relaxed = true)

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @Before
  fun setUp() {
    every { clerk.user } returns null
  }

  @After
  fun tearDown() {

    unmockkAll()
  }

  @Test
  fun connectExternalAccount_success_setsSuccessState() = runTest {
    val user = mockk<User>()
    val account = mockk<ExternalAccount>()
    every { clerk.user } returns user
    coEvery { user.createExternalAccount(any()) } returns account

    val viewModel = AddConnectedAccountViewModel(clerk)

    viewModel.connectExternalAccount(OAuthProvider.Github)
    advanceUntilIdle()

    assertEquals(AddConnectedAccountViewModel.State.Success, viewModel.state.value)
    coVerify(exactly = 0) { account.reauthorize(any()) }
  }

  @Test
  fun connectExternalAccount_preservesCustomProviderStrategy() = runTest {
    val customProvider = OAuthProvider.Unrecognized("custom_patreon")
    val user = mockk<User>()
    val account = mockk<ExternalAccount>()
    every { clerk.user } returns user
    coEvery { user.createExternalAccount(any()) } returns account

    val viewModel = AddConnectedAccountViewModel(clerk)

    viewModel.connectExternalAccount(customProvider)
    advanceUntilIdle()

    assertEquals(AddConnectedAccountViewModel.State.Success, viewModel.state.value)
    coVerify(exactly = 1) {
      user.createExternalAccount(
        match<CreateExternalAccountParams> {
          it.strategy?.rawValue == "oauth_custom_patreon"
        }
      )
    }
  }

  @Test
  fun connectExternalAccount_failure_setsErrorState() = runTest {
    val user = mockk<User>()
    every { clerk.user } returns user
    val error = testCoreError("fail")
    coEvery { user.createExternalAccount(any()) } throws error

    val viewModel = AddConnectedAccountViewModel(clerk)

    viewModel.connectExternalAccount(OAuthProvider.Github)
    advanceUntilIdle()

    assertEquals(AddConnectedAccountViewModel.State.Error("fail"), viewModel.state.value)
  }

  @Test
  fun connectExternalAccount_withoutUser_setsGuardError() = runTest {
    every { clerk.user } returns null

    val viewModel = AddConnectedAccountViewModel(clerk)

    viewModel.connectExternalAccount(OAuthProvider.Github)
    advanceUntilIdle()

    assertEquals(
      AddConnectedAccountViewModel.State.Error("User does not exist"),
      viewModel.state.value,
    )
  }

  @Test
  fun googleOneTapEnabled_connectsExternalAccountInsteadOfSigningIn() = runTest {
    val user = mockk<User>()
    val account = mockk<ExternalAccount>()
    every { clerk.user } returns user
    coEvery { user.createExternalAccount(any()) } returns account

    val viewModel = AddConnectedAccountViewModel(clerk)

    viewModel.connectExternalAccount(OAuthProvider.Google)
    advanceUntilIdle()

    assertEquals(AddConnectedAccountViewModel.State.Success, viewModel.state.value)
    coVerify(exactly = 1) {
      user.createExternalAccount(
        match<CreateExternalAccountParams> { it.strategy?.rawValue == "oauth_google" }
      )
    }
    coVerify(exactly = 0) { account.reauthorize(any()) }
    coVerify(exactly = 0) { clerk.authenticateWithSSO(any()) }
  }
}
