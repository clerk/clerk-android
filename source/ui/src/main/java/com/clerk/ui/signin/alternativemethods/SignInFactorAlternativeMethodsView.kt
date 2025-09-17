package com.clerk.ui.signin.alternativemethods

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.signin.alternativeFirstFactors
import com.clerk.api.signin.alternativeSecondFactors
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.toOAuthProvidersList
import com.clerk.ui.R
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.divider.TextDivider
import com.clerk.ui.signin.password.forgot.AlternativeFactorList
import com.clerk.ui.util.TextIconHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * A view that displays alternative sign-in methods.
 *
 * This component can be used for both first-factor and second-factor authentication steps, showing
 * a list of available methods like social providers (for first factor), or other verification
 * strategies (e.g., password, phone code).
 *
 * @param currentFactor The factor that the user is currently trying to authenticate with. This is
 *   used to determine which alternative factors to show.
 * @param onBackPressed A callback invoked when the user presses the back button.
 * @param modifier The [Modifier] to be applied to the view.
 * @param isSecondFactor A flag indicating whether the view is being used for a second-factor
 *   authentication step. This affects which alternative factors are fetched.
 * @param onClickFactor A callback invoked when the user selects an alternative factor from the
 *   list.
 */
@Composable
fun SignInFactorAlternativeMethodsView(
  currentFactor: Factor,
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  isSecondFactor: Boolean = false,
  onClickFactor: (Factor) -> Unit,
) {
  val socialProviders =
    if (isSecondFactor) emptyList() else Clerk.socialProviders.toOAuthProvidersList()
  val alternativeFactors =
    if (isSecondFactor) Clerk.signIn?.alternativeSecondFactors(currentFactor)
    else Clerk.signIn?.alternativeFirstFactors(currentFactor)

  SignInFactorAlternativeMethodsViewImpl(
    modifier = modifier,
    onBackPressed = onBackPressed,
    alternativeFactors = alternativeFactors.orEmpty().toImmutableList(),
    providers = socialProviders.toImmutableList(),
    onClickFactor = onClickFactor,
  )
}

/**
 * The internal implementation of the [SignInFactorAlternativeMethodsView].
 *
 * @param onBackPressed A callback invoked when the user presses the back button.
 * @param providers A list of social providers to display.
 * @param alternativeFactors A list of alternative factors (e.g., password, passkey) to display.
 * @param modifier The [Modifier] to be applied to the view.
 * @param textIconHelper A helper class to get the appropriate text and icon for each factor.
 * @param viewModel The [AlternativeMethodsViewModel] for handling the view's logic, such as social
 *   sign-in.
 * @param onClickFactor A callback invoked when the user selects an alternative factor.
 */
@Composable
private fun SignInFactorAlternativeMethodsViewImpl(
  onBackPressed: () -> Unit,
  providers: ImmutableList<OAuthProvider>,
  alternativeFactors: ImmutableList<Factor>,
  modifier: Modifier = Modifier,
  textIconHelper: TextIconHelper = TextIconHelper(),
  viewModel: AlternativeMethodsViewModel = viewModel(),
  onClickFactor: (Factor) -> Unit,
) {
  val context = LocalContext.current
  ClerkThemedAuthScaffold(
    modifier = modifier,
    onBackPressed = onBackPressed,
    title = stringResource(R.string.use_another_method),
    subtitle = "Facing issues? You can use any of these methods to sign in.",
  ) {
    if (providers.isNotEmpty()) {
      ClerkSocialRow(providers = providers, onClick = { viewModel.signInWithProvider(it) })
      Spacers.Vertical.Spacer24()
      TextDivider(text = stringResource(R.string.or))
      Spacers.Vertical.Spacer24()
    }
    AlternativeFactorList(
      alternativeFactors = alternativeFactors,
      textIconHelper = textIconHelper,
      context = context,
      onClickFactor = onClickFactor,
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  SignInFactorAlternativeMethodsViewImpl(
    onBackPressed = {},
    alternativeFactors =
      listOf(Factor(strategy = StrategyKeys.PASSWORD), Factor(strategy = StrategyKeys.PHONE_CODE))
        .toImmutableList(),
    onClickFactor = {},
    providers =
      listOf(OAuthProvider.GOOGLE, OAuthProvider.APPLE, OAuthProvider.FACEBOOK).toImmutableList(),
  )
}
