package com.clerk.ui.userprofile.email

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.clerk.ui.theme.ClerkMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserProfileAddEmailViewTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun emailInputSurvivesStateRestoration() {
    val restorationTester = StateRestorationTester(composeTestRule)
    restorationTester.setContent {
      ClerkMaterialTheme {
        UserProfileAddEmailViewBottomSheetContent(onDismiss = {}, onVerify = {})
      }
    }

    composeTestRule.onNode(hasSetTextAction()).performTextInput("person@example.com")
    restorationTester.emulateSavedInstanceStateRestore()

    composeTestRule.onNodeWithText("person@example.com").assertExists()
  }
}
