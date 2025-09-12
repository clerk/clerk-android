package com.clerk.ui.signin.passkey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfig
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.HeaderTextView
import com.clerk.ui.core.common.HeaderType
import com.clerk.ui.core.common.SecuredByClerk
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.common.dimens.dp18
import com.clerk.ui.core.common.dimens.dp72
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

@Composable
fun SignInFactorOnePasskeyView(
  factor: Factor,
  modifier: Modifier = Modifier,
  viewModel: PasskeyViewModel = viewModel(),
  onBackPressed: () -> Unit = {},
  onContinue: () -> Unit = {},
) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp18)
          .then(modifier),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      HeaderView(onBackPressed = onBackPressed, factor = factor, onContinue = onContinue)
      Spacers.Vertical.Spacer32()
      Icon(
        modifier = Modifier.size(dp72),
        painter = painterResource(R.drawable.ic_passkey),
        contentDescription = stringResource(R.string.passkey_icon),
        tint = ClerkMaterialTheme.colors.primary,
      )
      Spacers.Vertical.Spacer32()
      ClerkButton(
        text = stringResource(R.string.continue_text),
        onClick = { viewModel.authenticate() },
        modifier = Modifier.fillMaxWidth(),
        icons =
          ClerkButtonDefaults.icons(
            trailingIcon = R.drawable.ic_triangle_right,
            trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
          ),
      )
      Spacers.Vertical.Spacer16()
      ClerkTextButton(onClick = {}, text = stringResource(R.string.use_a_different_method))
      Spacers.Vertical.Spacer32()
      SecuredByClerk()
    }
  }
}

@Composable
private fun HeaderView(onBackPressed: () -> Unit, factor: Factor, onContinue: () -> Unit) {
  ClerkTopAppBar(onBackPressed = onBackPressed)
  Spacers.Vertical.Spacer8()
  HeaderTextView(text = stringResource(R.string.use_your_passkey), type = HeaderType.Title)
  Spacers.Vertical.Spacer8()
  HeaderTextView(
    text =
      stringResource(
        R.string
          .using_your_passkey_confirms_it_s_you_your_device_may_ask_for_your_fingerprint_face_or_screen_lock
      ),
    type = HeaderType.Subtitle,
  )
  Spacers.Vertical.Spacer8()
  ClerkButton(
    text = factor.safeIdentifier!!,
    onClick = onContinue,
    icons =
      ClerkButtonDefaults.icons(
        trailingIcon = R.drawable.ic_edit,
        trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
      ),
    configuration =
      ClerkButtonDefaults.configuration(
        style = ClerkButtonConfig.ButtonStyle.Secondary,
        emphasis = ClerkButtonConfig.Emphasis.High,
      ),
  )
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorOnePasskeyView() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  SignInFactorOnePasskeyView(
    factor = Factor(strategy = StrategyKeys.PASSKEY, safeIdentifier = "sam@clerk.dev")
  )
}
