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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp52
import com.clerk.ui.core.dimens.dp56
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.signin.code.VerificationState
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.coroutines.delay

/** Default timer length in seconds for code resend functionality. */
private const val DEFAULT_TIMER_LENGTH = 60

/** Default delay in milliseconds for timer countdown. */
private const val DEFAULT_DELAY = 1000L
private const val CARET_HEIGHT_FRACTION = 0.45f
private const val DEFAULT_OTP_LENGTH = 6

/**
 * A specialized input field component for entering one-time passwords (OTP) or verification codes.
 *
 * This component displays a row of individual boxes for each digit of the code, with visual
 * feedback for different states including error, success, and verification in progress. It includes
 * automatic resend functionality with a countdown timer.
 *
 * @param onTextChange Callback invoked when the OTP text changes. Receives the current OTP string.
 * @param onClickResend Callback invoked when the user clicks the resend code link.
 * @param modifier Optional [Modifier] to be applied to the component.
 * @param timerDuration The duration of the countdown timer in seconds for resending the code.
 *   Defaults to [DEFAULT_TIMER_LENGTH].
 * @param verificationState The current state of the verification process (e.g., Default, Verifying,
 *   Success, Error). Defaults to [VerificationState.Default].
 * @param showResend Whether to show the resend code link and timer. Defaults to `true`.
 */
@Composable
fun ClerkCodeInputField(
  onTextChange: (String) -> Unit,
  onClickResend: () -> Unit,
  modifier: Modifier = Modifier,
  timerDuration: Int = DEFAULT_TIMER_LENGTH,
  verificationState: VerificationState = VerificationState.Default,
  showResend: Boolean = true,
) {
  var timeLeft by remember { mutableIntStateOf(timerDuration) }

  LaunchedEffect(Unit) {
    while (timeLeft > 0) {
      delay(DEFAULT_DELAY)
      timeLeft--
    }
  }

  ClerkMaterialTheme {
    val selectionColors = rememberSelectionColors()

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        OtpField(
          onTextChange = onTextChange,
          modifier = modifier,
          verificationState = verificationState,
        )

        SupportingText(verificationState)
        if (showResend) {
          if (timeLeft > 0) {
            IconTextRow(text = stringResource(R.string.didn_t_receive_a_code_resend, timeLeft))
          } else {
            ResendCodeText(onClick = onClickResend)
          }
        }
      }
    }
  }
}

@Composable
private fun OtpField(
  onTextChange: (String) -> Unit,
  verificationState: VerificationState,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()
  val focusRequester = remember { FocusRequester() }
  var otpText by remember { mutableStateOf("") }
  LaunchedEffect(Unit) { focusRequester.requestFocus() }
  BasicTextField(
    interactionSource = interactionSource,
    value = otpText,
    onValueChange = { newValue ->
      val filtered = newValue.filter { it.isDigit() }.take(DEFAULT_OTP_LENGTH)
      otpText = filtered
      onTextChange(filtered)
    },
    keyboardOptions =
      KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
    singleLine = true,
    textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
    cursorBrush = SolidColor(Color.Transparent),
    modifier =
      Modifier.focusRequester(focusRequester)
        .semantics { contentType = ContentType.SmsOtpCode }
        .then(modifier),
    decorationBox = { innerTextField ->
      OtpBoxRow(
        otpText = otpText,
        isFocused = isFocused,
        innerTextField = innerTextField,
        isError = verificationState is VerificationState.Error,
      )
    },
  )
}

/**
 * Displays supporting text based on the [VerificationState] of the OTP input.
 *
 * @param verificationState The current state of the verification process.
 */
@Composable
private fun SupportingText(verificationState: VerificationState) {
  when (verificationState) {
    is VerificationState.Error -> {
      IconTextRow(
        leadingIconResId = R.drawable.ic_warning,
        leadingIconTint = ClerkMaterialTheme.colors.danger,
        text = stringResource(R.string.incorrect_verification_code),
        textColor = ClerkMaterialTheme.colors.danger,
      )
    }

    is VerificationState.Success -> {
      IconTextRow(
        leadingIconResId = R.drawable.ic_check_circle,
        leadingIconTint = ClerkMaterialTheme.colors.success,
        text = stringResource(R.string.success),
      )
    }

    is VerificationState.Verifying -> {
      VerifyingCodeRow()
    }
    else -> {}
  }
}

/**
 * Displays a link to resend the verification code.
 *
 * @param onClick Callback invoked when the resend link is clicked.
 */
@Composable
private fun ResendCodeText(onClick: () -> Unit) {
  val annotatedString = buildAnnotatedString {
    withStyle(style = SpanStyle(color = ClerkMaterialTheme.colors.mutedForeground)) {
      append(stringResource(R.string.didn_t_receive_a_code))
    }
    withStyle(style = SpanStyle(color = ClerkMaterialTheme.colors.primary)) {
      append(stringResource(R.string.resend))
    }
  }

  Text(
    modifier = Modifier.clickable { onClick() }.padding(vertical = dp24),
    text = annotatedString,
    style = ClerkMaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
  )
}

/** Displays a row indicating that the verification code is being verified. */
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
      text = stringResource(R.string.verifying),
      color = ClerkMaterialTheme.colors.mutedForeground,
      style = ClerkMaterialTheme.typography.bodyMedium,
    )
  }
}

/** Returns the text selection colors for the OTP input field. */
@Composable
private fun rememberSelectionColors(): TextSelectionColors {
  return TextSelectionColors(
    handleColor = ClerkMaterialTheme.colors.primary,
    backgroundColor = ClerkMaterialTheme.computedColors.inputBorder.copy(alpha = 0.3f),
  )
}

/**
 * Displays a row with an icon and text.
 *
 * @param text The text to display.
 * @param modifier Optional [Modifier] to be applied to the component.
 * @param leadingIconTint The color of the leading icon.
 * @param leadingIconResId The resource ID of the leading icon.
 * @param textColor The color of the text.
 */
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

/**
 * Displays a row of boxes for the OTP input.
 *
 * @param otpText The current OTP text.
 * @param isFocused Whether the OTP input field is focused.
 * @param innerTextField The inner text field.
 * @param isError Whether an error occurred.
 */
@Composable
private fun OtpBoxRow(
  otpText: String,
  isFocused: Boolean,
  innerTextField: @Composable () -> Unit,
  isError: Boolean,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(dp8),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Place the hidden inner text field at the start so the system paste/selection toolbar
    // anchors near the first box instead of at the end.
    Box(modifier = Modifier.size(dp1).alpha(0f).background(Color.Transparent)) { innerTextField() }

    val showCaret = isFocused && otpText.length < DEFAULT_OTP_LENGTH

    repeat(DEFAULT_OTP_LENGTH) { index ->
      val char = otpText.getOrNull(index)?.toString() ?: ""
      val isCurrentBox = if (showCaret) index == otpText.length else false

      OtpBox(char = char, isCurrentBox = isCurrentBox, isError)
    }
  }
}

/**
 * Displays a single box for the OTP input.
 *
 * @param char The character to display in the box.
 * @param isCurrentBox Whether this box is the current one.
 * @param isError Whether an error occurred.
 */
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

/** Displays a blinking caret in the OTP box. */
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
private fun Preview() {
  ClerkMaterialTheme {
    Box(
      modifier =
        Modifier.background(color = ClerkMaterialTheme.colors.background).padding(top = dp12)
    ) {
      ClerkCodeInputField(onTextChange = {}, onClickResend = {})
    }
  }
}
