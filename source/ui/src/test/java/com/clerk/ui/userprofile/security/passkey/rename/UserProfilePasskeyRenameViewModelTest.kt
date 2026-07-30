package com.clerk.ui.userprofile.security.passkey.rename

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.Passkey
import com.clerk.api.passkeys.update
import com.clerk.api.user.User
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfilePasskeyRenameViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    mockkStatic("com.clerk.api.passkeys.PasskeyKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.passkeys.PasskeyKt")
    unmockkAll()
  }

  @Test
  fun `successful rename state can be reset before reopening`() = runTest {
    val passkey = mockk<Passkey>()
    val user = mockk<User>()
    every { passkey.id } returns "passkey_123"
    every { user.passkeys } returns listOf(passkey)
    every { Clerk.user } returns user
    coEvery { passkey.update(name = "Work laptop") } returns ClerkResult.success(passkey)

    val viewModel = UserProfilePasskeyRenameViewModel()
    viewModel.state.test {
      assertEquals(UserProfilePasskeyRenameViewModel.State.Idle, awaitItem())

      viewModel.renamePasskey(passkeyId = "passkey_123", newName = "Work laptop")
      assertEquals(UserProfilePasskeyRenameViewModel.State.Success, awaitItem())

      viewModel.resetState()
      assertEquals(UserProfilePasskeyRenameViewModel.State.Idle, awaitItem())
    }
  }
}
