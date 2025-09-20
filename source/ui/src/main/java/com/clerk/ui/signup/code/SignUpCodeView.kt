package com.clerk.ui.signup.code

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.core.progress.ClerkLinearProgressIndicator
import com.clerk.ui.util.formattedAsPhoneNumberIfPossible

@Composable
fun SignUpCodeView(field: Field, modifier: Modifier = Modifier) {
  val title =
    when (field) {
      is Field.Phone -> stringResource(R.string.check_your_phone)
      is Field.Email -> stringResource(R.string.check_your_email)
    }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    title = title,
    hasLogo = false,
    identifier = field.value.formattedAsPhoneNumberIfPossible,
  ) {
    ClerkLinearProgressIndicator(progress = 0)
    Spacers.Vertical.Spacer32()
    ClerkCodeInputField(onTextChange = {}, onClickResend = {})
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  SignUpCodeView(field = Field.Phone("3012370655"))
}

sealed interface Field {
  val value: String

  data class Phone(override val value: String) : Field

  data class Email(override val value: String) : Field
}
