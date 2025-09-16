package com.clerk.ui.signin.password.forgot

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.signin.alternativeFirstFactors
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.divider.TextDivider
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.util.formattedAsPhoneNumberIfPossible
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun SignInFactorOneForgotPasswordView(
  factor: Factor,
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  onClickFactor: (Factor) -> Unit,
) {
  val socialProviders = Clerk.socialProviders
  val alternativeFactors = Clerk.signIn?.alternativeFirstFactors(factor)
  SignInFactorOneForgotPasswordViewImpl(
    onBackPressed = onBackPressed,
    socialProviders =
      socialProviders.map { OAuthProvider.fromStrategy(it.value.strategy) }.toImmutableList(),
    modifier = modifier,
    alternativeFactors = alternativeFactors.orEmpty().toImmutableList(),
    onClickFactor = onClickFactor,
  )
}

@Composable
private fun SignInFactorOneForgotPasswordViewImpl(
  onBackPressed: () -> Unit,
  alternativeFactors: ImmutableList<Factor>,
  socialProviders: ImmutableList<OAuthProvider>,
  modifier: Modifier = Modifier,
  viewModel: ForgotPasswordViewModel = viewModel(),
  onClickFactor: (Factor) -> Unit,
) {
  val context = LocalContext.current
  ClerkThemedAuthScaffold(
    modifier = modifier,
    onBackPressed = onBackPressed,
    hasLogo = false,
    title = stringResource(R.string.forgot_password),
  ) {
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.reset_your_password),
      onClick = {},
    )
    Spacers.Vertical.Spacer24()
    TextDivider(text = stringResource(R.string.or_sign_in_with_another_method))
    Spacers.Vertical.Spacer24()
    if (Clerk.socialProviders.isNotEmpty()) {
      ClerkSocialRow(providers = socialProviders, onClick = { viewModel.signInWithProvider(it) })
      Spacers.Vertical.Spacer24()
    }
    alternativeFactors.forEach {
      val actionText = actionText(it, context) ?: return@forEach
      val iconRes = iconResource(it)
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        icons =
          ClerkButtonDefaults.icons(
            leadingIcon = iconRes,
            leadingIconColor = ClerkMaterialTheme.colors.mutedForeground,
          ),
        text = actionText,
        configuration =
          ClerkButtonDefaults.configuration(
            style = ClerkButtonConfiguration.ButtonStyle.Secondary,
            emphasis = ClerkButtonConfiguration.Emphasis.High,
          ),
        onClick = { onClickFactor(it) },
      )
    }
  }
}

fun actionText(factor: Factor, context: Context): String? {
  return when (factor.strategy) {
    StrategyKeys.PHONE_CODE -> {
      val safeIdentifier = factor.safeIdentifier
      if (safeIdentifier.isNullOrBlank()) {
        context.getString(R.string.send_sms_code)
      } else {
        context.getString(
          R.string.send_sms_code_to_phone,
          safeIdentifier.formattedAsPhoneNumberIfPossible,
        )
      }
    }
    StrategyKeys.EMAIL_CODE -> {
      val safeIdentifier = factor.safeIdentifier
      if (safeIdentifier.isNullOrBlank()) {
        context.getString(R.string.email_code)
      } else {
        context.getString(R.string.email_code_to_email, safeIdentifier)
      }
    }
    StrategyKeys.PASSKEY -> context.getString(R.string.sign_in_with_your_passkey)
    StrategyKeys.PASSWORD -> context.getString(R.string.sign_in_with_your_password)
    StrategyKeys.TOTP -> context.getString(R.string.use_your_authenticator_app)
    StrategyKeys.BACKUP_CODE -> context.getString(R.string.use_a_backup_code)
    else -> null
  }
}

fun iconResource(factor: Factor): Int? {
  return when (factor.strategy) {
    StrategyKeys.PHONE_CODE -> R.drawable.ic_sms
    StrategyKeys.EMAIL_CODE -> R.drawable.ic_email
    StrategyKeys.PASSKEY -> R.drawable.ic_fingerprint
    StrategyKeys.PASSWORD -> R.drawable.ic_lock
    else -> null
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  SignInFactorOneForgotPasswordViewImpl(
    onBackPressed = {},
    socialProviders =
      listOf(OAuthProvider.GOOGLE, OAuthProvider.FACEBOOK, OAuthProvider.APPLE).toImmutableList(),
    alternativeFactors =
      listOf(
          Factor(StrategyKeys.PASSWORD),
          Factor(StrategyKeys.PASSKEY),
          Factor(strategy = StrategyKeys.EMAIL_CODE),
          Factor(strategy = StrategyKeys.PHONE_CODE, safeIdentifier = "3012370655"),
        )
        .toImmutableList(),
    onClickFactor = {},
  )
}
