package com.clerk.ui.core.input

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.clerk.ui.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClerkTextFieldTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun leadingIconTapFocusesField() {
    composeTestRule.setContent {
      ClerkTextField(value = "", onValueChange = {}, leadingIcon = R.drawable.ic_search)
    }

    composeTestRule.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
    composeTestRule.onNode(hasSetTextAction()).performTouchInput { click(Offset(24f, centerY)) }
    composeTestRule.onNode(hasSetTextAction()).assertIsFocused()
  }

  @Test
  fun customTrailingIconTapFocusesField() {
    composeTestRule.setContent {
      ClerkTextField(value = "", onValueChange = {}, trailingIcon = R.drawable.ic_cross)
    }

    composeTestRule.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
    composeTestRule.onNode(hasSetTextAction()).performTouchInput {
      click(Offset(width - 24f, centerY))
    }
    composeTestRule.onNode(hasSetTextAction()).assertIsFocused()
  }

  @Test
  fun passwordWarningIconIsDecorative() {
    composeTestRule.setContent {
      ClerkTextField(
        value = "password",
        onValueChange = {},
        isError = true,
        visualTransformation = PasswordVisualTransformation(),
      )
    }

    composeTestRule.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
  }

  @Test
  fun passwordVisibilityToggleRemainsInteractive() {
    composeTestRule.setContent {
      ClerkTextField(
        value = "password",
        onValueChange = {},
        visualTransformation = PasswordVisualTransformation(),
      )
    }

    composeTestRule.onNodeWithContentDescription("Show password").performClick()
    composeTestRule.onNodeWithContentDescription("Hide password").performClick()
    composeTestRule.onNodeWithContentDescription("Show password").assertExists()
  }
}
