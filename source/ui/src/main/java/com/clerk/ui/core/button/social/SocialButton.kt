package com.clerk.ui.core.button.social

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import coil3.compose.AsyncImage
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.logoUrl
import com.clerk.api.sso.providerName
import com.clerk.api.sso.setLogoUrl
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp3
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.LocalClerkColors
import com.clerk.ui.theme.LocalClerkDesign

@Composable
fun SocialButton(
  provider: OAuthProvider,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
  onClick: (OAuthProvider) -> Unit = {},
) {
  ClerkMaterialTheme {
    val design = LocalClerkDesign.current
    val colors = LocalClerkColors.current
    val shape = RoundedCornerShape(design.borderRadius)

    Button(
      enabled = isEnabled,
      onClick = { onClick(provider) },
      shape = shape,
      elevation = ButtonDefaults.buttonElevation(defaultElevation = dp3),
      colors =
        ButtonDefaults.buttonColors(
          // keep the container opaque; dim only content when disabled
          containerColor = MaterialTheme.colorScheme.background,
          contentColor = MaterialTheme.colorScheme.onBackground,
          disabledContainerColor = MaterialTheme.colorScheme.background,
          disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        ),
      contentPadding = ButtonDefaults.ContentPadding,
      modifier =
        modifier
          .shadow(
            elevation = dp3,
            shape = shape,
            clip = true,
            spotColor = colors.shadow!!.copy(alpha = 0.8f),
          )
          .defaultMinSize(minHeight = dp48),
    ) {
      Row(
        modifier =
          Modifier.background(Color.Transparent).padding(horizontal = dp12, vertical = dp6),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterHorizontally),
      ) {
        AsyncImage(
          model = provider.logoUrl,
          contentDescription = null,
          fallback = painterResource(R.drawable.ic_google),
          alpha = if (isEnabled) 1f else 0.5f,
        )
        Text(text = provider.providerName, style = MaterialTheme.typography.titleMedium)
      }
    }
  }
}

@SuppressLint("VisibleForTests")
@PreviewLightDark
@Composable
private fun PreviewSocialButton() {
  val provider = OAuthProvider.GOOGLE
  provider.setLogoUrl(null)
  ClerkMaterialTheme {
    Column(
      Modifier.background(MaterialTheme.colorScheme.background).padding(dp12),
      verticalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterVertically),
    ) {
      SocialButton(provider = provider)
      SocialButton(provider = provider, isEnabled = false)
    }
  }
}
