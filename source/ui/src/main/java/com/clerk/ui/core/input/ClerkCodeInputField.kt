package com.clerk.ui.core.input

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp52
import com.clerk.ui.core.dimens.dp56
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

// Constants
private const val CARET_HEIGHT_FRACTION = 0.45f

@Composable
fun ClerkCodeInputField(
  onOtpTextChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  otpLength: Int = 6,
) {
  ClerkMaterialTheme {
    var otpText by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val selectionColors = rememberSelectionColors()

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
      BasicTextField(
        interactionSource = interactionSource,
        value = otpText,
        onValueChange = { newValue ->
          val filtered = newValue.filter { it.isDigit() }.take(otpLength)
          if (filtered != otpText) {
            otpText = filtered
            onOtpTextChange(filtered)
          }
        },
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        singleLine = true,

        // Hide BasicTextField's own drawing so only our boxes show
        textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = Modifier.semantics { contentType = ContentType.SmsOtpCode }.then(modifier),
        decorationBox = { innerTextField ->
          OtpBoxRow(
            otpText = otpText,
            otpLength = otpLength,
            isFocused = isFocused,
            innerTextField = innerTextField,
          )
        },
      )
    }
  }
}

@Composable
private fun rememberSelectionColors(): TextSelectionColors {
  return TextSelectionColors(
    handleColor = ClerkMaterialTheme.colors.primary,
    backgroundColor = ClerkMaterialTheme.computedColors.inputBorder.copy(alpha = 0.3f),
  )
}

@Composable
private fun OtpBoxRow(
  otpText: String,
  otpLength: Int,
  isFocused: Boolean,
  innerTextField: @Composable () -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(dp8),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val showCaret = isFocused && otpText.length < otpLength

    repeat(otpLength) { index ->
      val char = otpText.getOrNull(index)?.toString() ?: ""
      val isCurrentBox = if (showCaret) index == otpText.length else false

      OtpBox(char = char, isCurrentBox = isCurrentBox)
    }

    // Invisible input field for IME/paste/accessibility
    Box(modifier = Modifier.size(dp1).alpha(0f).background(Color.Transparent)) { innerTextField() }
  }
}

@Composable
private fun OtpBox(char: String, isCurrentBox: Boolean) {
  val boxShape = ClerkMaterialTheme.shape
  val borderColor =
    if (isCurrentBox) ClerkMaterialTheme.colors.primary
    else ClerkMaterialTheme.computedColors.inputBorder

  Box(
    modifier =
      Modifier.height(dp56)
        .width(dp52)
        .clip(boxShape)
        .background(color = ClerkMaterialTheme.colors.input.copy(alpha = 1f), shape = boxShape)
        .border(width = dp1, color = borderColor, shape = boxShape),
    contentAlignment = Alignment.Center,
  ) {
    if (char.isNotEmpty()) {
      Text(
        text = char,
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.foreground,
      )
    } else if (isCurrentBox) {
      BlinkingCaret()
    }
  }
}

@Composable
private fun BlinkingCaret() {
  val blinkAlpha by
    rememberInfiniteTransition(label = "otp-caret")
      .animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
          ),
        label = "otp-caret-alpha",
      )

  Box(
    modifier =
      Modifier.width(dp1)
        .fillMaxHeight(CARET_HEIGHT_FRACTION)
        .alpha(blinkAlpha)
        .background(ClerkMaterialTheme.colors.primary)
  )
}

@PreviewLightDark
@Composable
private fun PreviewClerkCodeInputField() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background).padding(dp16)) {
      ClerkCodeInputField(onOtpTextChange = {})
    }
  }
}
