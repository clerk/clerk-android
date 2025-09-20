package com.clerk.ui.signup

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.core.progress.ClerkLinearProgressIndicator

@Composable
fun SignUpCodeView(field: Field, modifier: Modifier = Modifier) {
  ClerkThemedAuthScaffold(
    modifier = modifier,
    title = "Check your email",
    hasLogo = false,
    identifier = field.value,
  ) {
    ClerkLinearProgressIndicator(progress = 0)
    Spacers.Vertical.Spacer32()
    ClerkCodeInputField(onTextChange = {}, onClickResend = {})
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  SignUpCodeView(field = Field.Email("example@email.com"))
}

sealed interface Field {
  val value: String

  data class Phone(override val value: String) : Field

  data class Email(override val value: String) : Field
}
