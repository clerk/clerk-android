package com.clerk.ui.core.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfig
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.signin.code.SignInFactorCodeHelper
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun AuthViewHeader(factor: Factor, modifier: Modifier = Modifier) {
  Column(modifier = Modifier.then(modifier)) {
    HeaderTextView(text = SignInFactorCodeHelper.titleForStrategy(factor), type = HeaderType.Title)
    Spacers.Vertical.Spacer8()
    HeaderTextView(
      text = SignInFactorCodeHelper.subtitleForStrategy(factor),
      type = HeaderType.Subtitle,
    )
    Spacers.Vertical.Spacer8()
    ClerkButton(
      text = factor.safeIdentifier.orEmpty(),
      onClick = {},
      modifier = Modifier.wrapContentHeight(),
      buttonConfig = ClerkButtonConfig(style = ClerkButtonConfig.ButtonStyle.Secondary),
      icons =
        ClerkButtonDefaults.icons(
          trailingIcon = R.drawable.ic_edit,
          trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
        ),
    )
  }
}
