package com.clerk.ui.core.input

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp52
import com.clerk.ui.core.dimens.dp56
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

// Constants
private const val CARET_HEIGHT_FRACTION = 0.45f

@Composable
fun ClerkCodeInputField(
  onOtpTextChange: (String) -> Unit,
  secondsLeft: Int,
  modifier: Modifier = Modifier,
  otpLength: Int = 6,
  isError: Boolean = false,
  isSuccess: Boolean = false,
  isVerifying: Boolean = false,
  onClickResend: () -> Unit,
) {
  ClerkMaterialTheme {
    var otpText by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val selectionColors = rememberSelectionColors()

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
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
          textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
          cursorBrush = SolidColor(Color.Transparent),
          modifier = Modifier.semantics { contentType = ContentType.SmsOtpCode }.then(modifier),
          decorationBox = { innerTextField ->
            OtpBoxRow(
              otpText = otpText,
              otpLength = otpLength,
              isFocused = isFocused,
              innerTextField = innerTextField,
              isError = isError,
            )
          },
        )
        SupportingText(isError, isSuccess, isVerifying)
        if (secondsLeft > 0) {
          IconTextRow(text = stringResource(R.string.didn_t_receive_a_code_resend, secondsLeft))
        } else {
          ResendCodeText(onClick = onClickResend)
        }
      }
    }
  }
}

@Composable
private fun SupportingText(isError: Boolean, isSuccess: Boolean, isVerifying: Boolean) {
  when {
    isError -> {
      IconTextRow(
        leadingIconResId = R.drawable.ic_warning,
        leadingIconTint = ClerkMaterialTheme.colors.danger,
        text = stringResource(R.string.incorrect_verification_code),
        textColor = ClerkMaterialTheme.colors.danger,
      )
    }

    isSuccess -> {
      IconTextRow(
        leadingIconResId = R.drawable.ic_check_circle,
        leadingIconTint = ClerkMaterialTheme.colors.success,
        text = stringResource(R.string.success),
      )
    }

    isVerifying -> {
      VerifyingCodeRow()
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResendCodeText(onClick: () -> Unit) {
  val annotatedString = buildAnnotatedString {
    withStyle(style = SpanStyle(color = ClerkMaterialTheme.colors.mutedForeground)) {
      append("Didn't receive a code? ")
    }
    withStyle(style = SpanStyle(color = ClerkMaterialTheme.colors.primary)) { append("Resend") }
  }

  Text(
    modifier = Modifier.clickable { onClick() }.padding(top = dp24),
    text = annotatedString,
    style = ClerkMaterialTheme.typography.titleSmallEmphasized,
  )
}

@Composable
private fun VerifyingCodeRow() {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = dp24),
    horizontalArrangement = Arrangement.spacedBy(dp4, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CircularProgressIndicator(
      modifier = Modifier.size(dp12),
      color = ClerkMaterialTheme.colors.primary,
      strokeWidth = dp1,
    )
    Text(
      text = "Verifying...",
      color = ClerkMaterialTheme.colors.mutedForeground,
      style = ClerkMaterialTheme.typography.bodyMedium,
    )
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
private fun IconTextRow(
  text: String,
  modifier: Modifier = Modifier,
  leadingIconTint: Color? = null,
  @DrawableRes leadingIconResId: Int? = null,
  textColor: Color = ClerkMaterialTheme.colors.mutedForeground,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = dp24).then(modifier),
    horizontalArrangement = Arrangement.spacedBy(dp4, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    leadingIconResId?.let {
      Icon(painter = painterResource(it), contentDescription = null, tint = leadingIconTint!!)
    }
    Text(text = text, color = textColor, style = ClerkMaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun OtpBoxRow(
  otpText: String,
  otpLength: Int,
  isFocused: Boolean,
  innerTextField: @Composable () -> Unit,
  isError: Boolean,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(dp8),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val showCaret = isFocused && otpText.length < otpLength

    repeat(otpLength) { index ->
      val char = otpText.getOrNull(index)?.toString() ?: ""
      val isCurrentBox = if (showCaret) index == otpText.length else false

      OtpBox(char = char, isCurrentBox = isCurrentBox, isError)
    }

    Box(modifier = Modifier.size(dp1).alpha(0f).background(Color.Transparent)) { innerTextField() }
  }
}

@Composable
private fun OtpBox(char: String, isCurrentBox: Boolean, isError: Boolean) {
  val boxShape = ClerkMaterialTheme.shape
  val borderColor =
    when {
      isError -> ClerkMaterialTheme.colors.danger
      isCurrentBox -> ClerkMaterialTheme.colors.primary
      else -> ClerkMaterialTheme.computedColors.inputBorder
    }

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
    Column(
      modifier = Modifier.background(ClerkMaterialTheme.colors.background).padding(dp16),
      verticalArrangement = Arrangement.spacedBy(dp16),
    ) {
      ClerkCodeInputField(onOtpTextChange = {}, secondsLeft = 30, onClickResend = {})
      ClerkCodeInputField(
        onOtpTextChange = {},
        isError = true,
        secondsLeft = 30,
        onClickResend = {},
      )
      ClerkCodeInputField(
        onOtpTextChange = {},
        isSuccess = true,
        secondsLeft = 0,
        onClickResend = {},
      )
      ClerkCodeInputField(
        onOtpTextChange = {},
        isVerifying = true,
        secondsLeft = 0,
        onClickResend = {},
      )
    }
  }
}
