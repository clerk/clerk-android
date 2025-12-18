package com.clerk.ui.signin.clienttrust

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider

/**
 * A composable that displays the client trust verification view.
 *
 * This view is shown when the user is signing in from a new or untrusted device and needs to
 * complete an additional verification step. It displays a warning message explaining why
 * verification is needed, followed by the code input for verification.
 *
 * Based on the `strategy` of the provided [factor], this composable will delegate rendering to
 * [SignInFactorCodeView] for phone code or email code verification, or [SignInGetHelpView] for
 * unsupported strategies.
 *
 * @param factor The factor to be verified for client trust.
 * @param modifier The [Modifier] to be applied to the view.
 * @param clerkTheme Optional theme override for customization.
 * @param onAuthComplete Callback invoked when authentication is complete.
 */
@Composable
fun SignInClientTrustView(
  factor: Factor,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    when (factor.strategy) {
      StrategyKeys.PHONE_CODE,
      StrategyKeys.EMAIL_CODE ->
        SignInFactorCodeView(
          factor = factor,
          isSecondFactor = true,
          isClientTrust = true,
          modifier = modifier,
          onAuthComplete = onAuthComplete,
        )
      else -> SignInGetHelpView(modifier = modifier)
    }
  }
}

/**
 * A composable that displays a warning message for client trust verification.
 *
 * This message informs the user that they are signing in from a new device and explains why
 * additional verification is being requested.
 */
@Composable
internal fun ClientTrustWarningMessage(modifier: Modifier = Modifier) {
  Text(
    text = stringResource(R.string.signing_in_from_new_device),
    color = ClerkMaterialTheme.colors.warning,
    style = ClerkMaterialTheme.typography.bodySmall,
    textAlign = TextAlign.Center,
    modifier = modifier.padding(bottom = dp16),
  )
}

@PreviewLightDark
@Composable
private fun Preview() {
  PreviewAuthStateProvider {
    SignInClientTrustView(
      factor = Factor(StrategyKeys.EMAIL_CODE, safeIdentifier = "user@example.com"),
      onAuthComplete = {},
    )
  }
}
