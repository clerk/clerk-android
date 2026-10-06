package com.clerk.ui.userprofile.email

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.network.model.verification.Verification
import com.clerk.ui.theme.ClerkMaterialTheme
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserProfileEmailRowViewModelTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    mockkObject(Clerk)
    makeEmailActionsFailWithoutNetwork()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun eachEmailRowOwnsItsViewModel() {
    val owner =
      object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
      }
    val firstRowErrors = mutableListOf<String>()
    val secondRowErrors = mutableListOf<String>()
    val first = emailAddress("email_1", "first@example.com")
    val second = emailAddress("email_2", "second@example.com")

    composeTestRule.setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        ClerkMaterialTheme {
          Column {
            UserProfileEmailRow(
              emailAddress = first,
              onError = { firstRowErrors += it },
              onVerify = {},
            )
            UserProfileEmailRow(
              emailAddress = second,
              onError = { secondRowErrors += it },
              onVerify = {},
            )
          }
        }
      }
    }
    composeTestRule.waitForIdle()

    assertEquals(
      setOf("user-profile-email-row-email_1", "user-profile-email-row-email_2"),
      owner.viewModelStore.keys(),
    )
    val firstRowViewModel =
      ViewModelProvider(owner)["user-profile-email-row-email_1", EmailViewModel::class.java]

    composeTestRule.runOnIdle { firstRowViewModel.setAsPrimary(first) }
    composeTestRule.waitForIdle()

    assertEquals(1, firstRowErrors.size)
    assertEquals(0, secondRowErrors.size)
  }

  private fun makeEmailActionsFailWithoutNetwork() {
    every { Clerk.isEmailImmutable } returns true
  }

  private fun emailAddress(id: String, address: String) =
    EmailAddress(
      id = id,
      emailAddress = address,
      verification = Verification(status = Verification.Status.VERIFIED),
    )
}
