package com.clerk.ui.userprofile

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.clerk.ui.userprofile.account.UserProfileAccountViewModel
import com.clerk.ui.userprofile.account.UserProfileAccountViewModel.DeleteAccountState
import com.clerk.ui.userprofile.account.UserProfileDeleteAccountConfirmationView
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserProfileStateTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun deleteAccountSuccessKeepsRootEntryInNavDisplay() {
    val deleteState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val viewModel =
      mockk<UserProfileAccountViewModel>(relaxed = true) {
        every { deleteAccountStateFlow } returns deleteState
      }
    val backStack =
      NavBackStack<NavKey>(
        UserProfileDestination.UserProfileAccount,
        UserProfileDestination.UserProfileSecurity,
      )

    composeTestRule.setContent {
      UserProfileStateProvider(backStack) {
        NavDisplay(
          backStack = backStack,
          entryProvider =
            entryProvider {
              entry<UserProfileDestination.UserProfileAccount> {}
              entry<UserProfileDestination.UserProfileSecurity> {
                UserProfileDeleteAccountConfirmationView(
                  onError = {},
                  viewModel = viewModel,
                  onClose = {},
                )
              }
            },
        )
      }
    }
    composeTestRule.runOnUiThread { deleteState.value = DeleteAccountState.Success }
    composeTestRule.waitForIdle()

    assertEquals(listOf(UserProfileDestination.UserProfileAccount), backStack.toList())
  }
}
