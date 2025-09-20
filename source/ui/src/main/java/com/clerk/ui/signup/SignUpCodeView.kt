package com.clerk.ui.signup

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.dimens.dp12
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SignUpCodeView(field: Field, modifier: Modifier = Modifier) {
  ClerkThemedAuthScaffold(
    modifier = modifier,
    title = "Check your email",
    hasLogo = false,
    identifier = field.value,
  ) {
    LinearProgressIndicator(
      progress = { 0.50f },
      color = ClerkMaterialTheme.colors.primary,
      gapSize = dp12,
      trackColor = ClerkMaterialTheme.colors.neutral.copy(alpha = .11f),
      drawStopIndicator = {},
    )
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
