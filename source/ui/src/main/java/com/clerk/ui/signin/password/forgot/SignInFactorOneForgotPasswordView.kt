package com.clerk.ui.signin.password.forgot

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.signin.alternativeFirstFactors
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.toOAuthProvidersList
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.divider.TextDivider
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.util.TextIconHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * A view that allows the user to initiate the forgot password flow. It also presents alternative
 * sign-in methods like social providers or other factors.
 *
 * @param factor The initial factor that led to this screen (typically password).
 * @param onBackPressed A callback to be invoked when the user presses the back button.
 * @param modifier The [Modifier] to be applied to the view.
 * @param onClickFactor A callback to be invoked when the user selects an alternative factor.
 */
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
    socialProviders = socialProviders.toOAuthProvidersList().toImmutableList(),
    modifier = modifier,
    alternativeFactors = alternativeFactors.orEmpty().toImmutableList(),
    onClickFactor = onClickFactor,
  )
}

/**
 * The internal implementation of the [SignInFactorOneForgotPasswordView].
 *
 * @param onBackPressed A callback to be invoked when the user presses the back button.
 * @param alternativeFactors A list of alternative factors the user can use to sign in.
 * @param socialProviders A list of social providers available for sign-in.
 * @param modifier The [Modifier] to be applied to the view.
 * @param viewModel The [ForgotPasswordViewModel] for handling the view's logic.
 * @param onClickFactor A callback to be invoked when the user selects an alternative factor.
 */
@Composable
private fun SignInFactorOneForgotPasswordViewImpl(
  onBackPressed: () -> Unit,
  alternativeFactors: ImmutableList<Factor>,
  socialProviders: ImmutableList<OAuthProvider>,
  modifier: Modifier = Modifier,
  viewModel: ForgotPasswordViewModel = viewModel(),
  textIconHelper: TextIconHelper = TextIconHelper(),
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
    AlternativeFactorList(alternativeFactors, textIconHelper, context, onClickFactor)
  }
}

@Composable
internal fun AlternativeFactorList(
  alternativeFactors: ImmutableList<Factor>,
  textIconHelper: TextIconHelper,
  context: Context,
  onClickFactor: (Factor) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(dp24, alignment = Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    alternativeFactors.forEach {
      val actionText = textIconHelper.actionText(it, context) ?: return@forEach
      val iconRes = textIconHelper.iconResource(it)

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
