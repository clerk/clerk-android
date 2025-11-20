package com.clerk.ui.signin.password.forgot

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.signin.alternativeFirstFactors
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.toOAuthProvidersList
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.auth.AuthDestination
import com.clerk.ui.auth.LocalAuthState
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.divider.TextDivider
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.util.TextIconHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * A view that allows the user to initiate the forgot password flow. It also presents alternative
 * sign-in methods like social providers or other factors.
 *
 * @param modifier The [Modifier] to be applied to the view.
 * @param onClickFactor A callback to be invoked when the user selects an alternative factor.
 */
@Composable
fun SignInFactorOneForgotPasswordView(
  onClickFactor: (Factor) -> Unit,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    val socialProviders = Clerk.socialProviders
    val alternativeFactors = Clerk.signIn?.alternativeFirstFactors()
    SignInFactorOneForgotPasswordViewImpl(
      socialProviders = socialProviders.toOAuthProvidersList().toImmutableList(),
      modifier = modifier,
      alternativeFactors = alternativeFactors.orEmpty().toImmutableList(),
      onClickFactor = onClickFactor,
      onAuthComplete = onAuthComplete,
    )
  }
}

/**
 * The internal implementation of the [SignInFactorOneForgotPasswordView].
 *
 * @param alternativeFactors A list of alternative factors the user can use to sign in.
 * @param socialProviders A list of social providers available for sign-in.
 * @param modifier The [Modifier] to be applied to the view.
 * @param viewModel The [ForgotPasswordViewModel] for handling the view's logic.
 * @param onClickFactor A callback to be invoked when the user selects an alternative factor.
 */
@Composable
private fun SignInFactorOneForgotPasswordViewImpl(
  alternativeFactors: ImmutableList<Factor>,
  socialProviders: ImmutableList<OAuthProvider>,
  onClickFactor: (Factor) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ForgotPasswordViewModel = viewModel(),
  textIconHelper: TextIconHelper = TextIconHelper(),
  onAuthComplete: () -> Unit,
) {
  val authState = LocalAuthState.current
  val context = LocalContext.current
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(state) {
    when (val s = state) {
      is ResetPasswordViewState.Error ->
        snackbarHostState.showSnackbar(
          s.message ?: context.getString(R.string.something_went_wrong_please_try_again)
        )
      ResetPasswordViewState.NotStarted -> authState.clearBackStack()
      is ResetPasswordViewState.ResetFactor ->
        authState.navigateTo(AuthDestination.SignInFactorOne(factor = s.factor))
      is ResetPasswordViewState.Success.SignIn ->
        authState.setToStepForStatus(s.signIn, onAuthComplete)
      is ResetPasswordViewState.Success.SignUp ->
        authState.setToStepForStatus(s.signUp, onAuthComplete)
      else -> {}
    }
  }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    onBackPressed = authState::navigateBack,
    hasLogo = false,
    title = stringResource(R.string.forgot_password),
  ) {
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.reset_your_password),
      onClick = { viewModel.resetPassword() },
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
  PreviewAuthStateProvider {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
    SignInFactorOneForgotPasswordViewImpl(
      socialProviders =
        persistentListOf(OAuthProvider.GOOGLE, OAuthProvider.FACEBOOK, OAuthProvider.APPLE),
      alternativeFactors =
        persistentListOf(
          Factor(StrategyKeys.PASSWORD),
          Factor(StrategyKeys.PASSKEY),
          Factor(strategy = StrategyKeys.EMAIL_CODE),
          Factor(strategy = StrategyKeys.PHONE_CODE, safeIdentifier = "3012370655"),
        ),
      onClickFactor = {},
      onAuthComplete = {},
    )
  }
}
