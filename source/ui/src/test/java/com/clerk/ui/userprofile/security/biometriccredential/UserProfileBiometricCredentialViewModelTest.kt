package com.clerk.ui.userprofile.security.biometriccredential

import com.clerk.api.biometriccredential.BiometricCredentialAvailability
import com.clerk.api.biometriccredential.BiometricCredentials
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileBiometricCredentialViewModelTest {

  private val dispatcher = UnconfinedTestDispatcher()
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `stale refresh cannot overwrite successful revocation`() =
    runTest(dispatcher) {
      val staleRefreshResult = CompletableDeferred<BiometricCredentialAvailability>()
      var availabilityRequestCount = 0
      mockkObject(BiometricCredentials)
      every { BiometricCredentials.currentUserLocalAvailability() } returns
        BiometricCredentialAvailability.Available
      coEvery { BiometricCredentials.currentUserAvailability() } coAnswers
        {
          if (availabilityRequestCount++ == 0) {
            staleRefreshResult.await()
          } else {
            BiometricCredentialAvailability.Unavailable(
              BiometricCredentialAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
            )
          }
        }
      coEvery { BiometricCredentials.revokeCurrentBiometricCredential() } returns
        ClerkResult.success(Unit)
      val viewModel = UserProfileBiometricCredentialViewModel(workDispatcher = dispatcher)

      viewModel.refreshAvailability()
      viewModel.setBiometricSignInEnabled(
        enabled = false,
        promptTitle = "Disable biometric sign-in",
        promptSubtitle = null,
      )
      viewModel.state.first { !it.isLoading && !it.isEnabled }

      staleRefreshResult.complete(BiometricCredentialAvailability.Available)

      assertFalse(viewModel.state.value.isEnabled)
    }
}
