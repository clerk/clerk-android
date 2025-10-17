package com.clerk.ui.userprofile.security.delete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileButtonRow

@Composable
fun UserProfileDeleteAccountSection(modifier: Modifier = Modifier, onDeleteAccount: () -> Unit) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp24)
          .padding(top = dp32)
          .then(modifier)
    ) {
      Text(
        text = stringResource(R.string.account).uppercase(),
        style = ClerkMaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      UserProfileButtonRow(
        textColor = ClerkMaterialTheme.colors.danger,
        text = stringResource(R.string.delete_account),
        onClick = onDeleteAccount,
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileDeleteAccountSection(onDeleteAccount = {})
}
