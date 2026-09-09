package com.clerk.ui.userprofile.security.passkey.rename

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.Field
import com.clerk.api.Partialtype
import com.clerk.api.Passkey
import com.clerk.api.User
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfilePasskeyRenameViewModelTest {
  private val clerk = mockk<Clerk>(relaxed = true)

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest fun setUp() {}

  @AfterTest
  fun tearDown() {

    unmockkAll()
  }

  @Test
  fun `successful rename state can be reset before reopening`() = runTest {
    val passkey = mockk<Passkey>()
    val user = mockk<User>()
    every { passkey.id } returns "passkey_123"
    every { user.passkeys } returns listOf(passkey)
    every { clerk.user } returns user
    coEvery { passkey.update(Partialtype(name = Field.Value("Work laptop"))) } returns passkey

    val viewModel = UserProfilePasskeyRenameViewModel(clerk)
    viewModel.state.test {
      assertEquals(UserProfilePasskeyRenameViewModel.State.Idle, awaitItem())

      viewModel.renamePasskey(passkeyId = "passkey_123", newName = "Work laptop")
      assertEquals(UserProfilePasskeyRenameViewModel.State.Loading, awaitItem())
      assertEquals(UserProfilePasskeyRenameViewModel.State.Success, awaitItem())

      viewModel.resetState()
      assertEquals(UserProfilePasskeyRenameViewModel.State.Idle, awaitItem())
    }
  }
}
