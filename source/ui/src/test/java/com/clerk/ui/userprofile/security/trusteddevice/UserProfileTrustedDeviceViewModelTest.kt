package com.clerk.ui.userprofile.security.trusteddevice

import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.trusteddevice.TrustedDeviceAvailability
import com.clerk.api.trusteddevice.TrustedDevices
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
class UserProfileTrustedDeviceViewModelTest {

  private val dispatcher = UnconfinedTestDispatcher()
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `stale refresh cannot overwrite successful revocation`() =
    runTest(dispatcher) {
      val staleRefreshResult = CompletableDeferred<TrustedDeviceAvailability>()
      var availabilityRequestCount = 0
      mockkObject(TrustedDevices)
      every { TrustedDevices.currentUserLocalAvailability() } returns
        TrustedDeviceAvailability.Available
      coEvery { TrustedDevices.currentUserAvailability() } coAnswers
        {
          if (availabilityRequestCount++ == 0) {
            staleRefreshResult.await()
          } else {
            TrustedDeviceAvailability.Unavailable(
              TrustedDeviceAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
            )
          }
        }
      coEvery { TrustedDevices.revokeCurrentDeviceCredential() } returns ClerkResult.success(Unit)
      val viewModel = UserProfileTrustedDeviceViewModel(workDispatcher = dispatcher)

      viewModel.refreshAvailability()
      viewModel.setTrustedDeviceSignInEnabled(
        enabled = false,
        promptTitle = "Disable biometric sign-in",
        promptSubtitle = null,
      )
      viewModel.state.first { !it.isLoading && !it.isEnabled }

      staleRefreshResult.complete(TrustedDeviceAvailability.Available)

      assertFalse(viewModel.state.value.isEnabled)
    }
}
