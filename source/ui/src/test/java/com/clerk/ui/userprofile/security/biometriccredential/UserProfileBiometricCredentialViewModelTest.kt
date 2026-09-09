package com.clerk.ui.userprofile.security.biometriccredential

import com.clerk.api.*
import com.clerk.api.BiometricCredentialAvailability
import com.clerk.testing.mockClerk
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
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
  private val clerk = mockClerk()

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
      coEvery { clerk.biometricCredentials.localAvailability(any()) } returns
        BiometricCredentialAvailability(true, null)
      coEvery { clerk.biometricCredentials.availability(any()) } coAnswers
        {
          if (availabilityRequestCount++ == 0) {
            staleRefreshResult.await()
          } else {
            BiometricCredentialAvailability(
              false,
              BiometricCredentialUnavailableReason.NoLocalCredential,
            )
          }
        }
      coEvery { clerk.biometricCredentials.revokeCurrentDeviceCredential() } returns null
      val viewModel = UserProfileBiometricCredentialViewModel(clerk)

      viewModel.refreshAvailability()
      viewModel.setBiometricSignInEnabled(
        enabled = false,
        promptTitle = "Disable biometric sign-in",
        promptSubtitle = null,
      )
      viewModel.state.first { !it.isLoading && !it.isEnabled }

      staleRefreshResult.complete(BiometricCredentialAvailability(true, null))

      assertFalse(viewModel.state.value.isEnabled)
    }
}
