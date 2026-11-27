package com.clerk.ui.userbutton

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.user.User
import com.clerk.ui.R
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PendingSessionAccountSheetTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var auth: Auth

  @Before
  fun setup() {
    auth = mockk()
    mockkObject(Clerk)
    every { Clerk.auth } returns auth
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `signOut failure does not show an error when the user is already signed out`() {
    val session = testPendingSession()
    val user = testUser()
    var signedIn = true
    var dismissed = false
    every { Clerk.isSignedIn } answers { signedIn }
    coEvery { auth.signOut(sessionId = session.id) } coAnswers
      {
        signedIn = false
        ClerkResult.Failure(
          ClerkErrorResponse(errors = listOf(Error(longMessage = "Network error")))
        )
      }

    composeTestRule.setContent {
      PendingSessionAccountSheet(
        user = user,
        session = session,
        onDismissRequest = { dismissed = true },
      )
    }

    val signOutLabel = RuntimeEnvironment.getApplication().getString(R.string.sign_out)
    composeTestRule.onNodeWithText(signOutLabel).performClick()
    composeTestRule.waitForIdle()

    assertTrue(dismissed)
    composeTestRule.onNodeWithText("Network error").assertDoesNotExist()
  }

  @Test
  fun `signOut failure shows an error when the user is still signed in`() {
    val session = testPendingSession()
    val user = testUser()
    var dismissed = false
    every { Clerk.isSignedIn } returns true
    coEvery { auth.signOut(sessionId = session.id) } returns
      ClerkResult.Failure(ClerkErrorResponse(errors = listOf(Error(longMessage = "Network error"))))

    composeTestRule.setContent {
      PendingSessionAccountSheet(
        user = user,
        session = session,
        onDismissRequest = { dismissed = true },
      )
    }

    val signOutLabel = RuntimeEnvironment.getApplication().getString(R.string.sign_out)
    composeTestRule.onNodeWithText(signOutLabel).performClick()
    composeTestRule.waitForIdle()

    assertFalse(dismissed)
    composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
  }

  private fun testPendingSession(): Session =
    Session(
      id = "sess_pending",
      status = Session.SessionStatus.PENDING,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      createdAt = 1_000,
      updatedAt = 1_000,
    )

  private fun testUser(): User =
    mockk(relaxed = true) {
      every { imageUrl } returns ""
      every { firstName } returns "Sam"
      every { lastName } returns "Clerk"
      every { username } returns "sam"
      every { primaryEmailAddress } returns null
    }
}
