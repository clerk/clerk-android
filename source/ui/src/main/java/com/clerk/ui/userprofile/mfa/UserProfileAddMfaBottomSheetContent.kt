package com.clerk.ui.userprofile.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.navigation3.runtime.rememberNavBackStack
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.PreviewUserProfileStateProvider
import com.clerk.ui.userprofile.UserProfileDestination
import com.clerk.ui.userprofile.UserProfileStateProvider
import com.clerk.ui.userprofile.account.UserProfileIconActionRow

@Composable
internal fun UserProfileAddMfaBottomSheetContent(
  mfaPhoneCodeIsEnabled: Boolean,
  mfaAuthenticatorAppIsEnabled: Boolean,
  modifier: Modifier = Modifier,
) {
  val userProfileState = LocalUserProfileState.current
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .then(modifier)
    ) {
      Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = stringResource(R.string.add_two_step_verification),
        color = ClerkMaterialTheme.colors.foreground,
        style = ClerkMaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      )
      Spacers.Vertical.Spacer24()
      Text(
        modifier = Modifier.padding(horizontal = dp24),
        text =
          stringResource(R.string.choose_how_you_d_like_to_receive_your_two_step_verification_code),
        color = ClerkMaterialTheme.colors.mutedForeground,
        style = ClerkMaterialTheme.typography.bodyMedium,
      )
      Spacers.Vertical.Spacer12()
      HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
      if (mfaPhoneCodeIsEnabled) {
        UserProfileIconActionRow(
          iconSize = dp24,
          iconResId = R.drawable.ic_phone,
          text = stringResource(R.string.sms_code),
          onClick = { userProfileState.navigateTo(UserProfileDestination.AddMfaView(ViewType.Sms)) },
        )
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
      }
      if (mfaAuthenticatorAppIsEnabled) {
        UserProfileIconActionRow(
          iconSize = dp24,
          iconResId = R.drawable.ic_key,
          text = stringResource(R.string.authenticator_application),
          onClick = {
            userProfileState.navigateTo(
              UserProfileDestination.AddMfaView(ViewType.AuthenticatorApp)
            )
          },
        )
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
      }
      Spacers.Vertical.Spacer24()
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewBottomSheet() {
  val backStack = rememberNavBackStack(UserProfileDestination.UserProfileSecurity)
  UserProfileStateProvider(backStack) {
    ClerkMaterialTheme {
      UserProfileAddMfaBottomSheetContent(
        mfaPhoneCodeIsEnabled = true,
        mfaAuthenticatorAppIsEnabled = true,
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewBottomSheetPhoneDisabled() {
  PreviewUserProfileStateProvider {
    ClerkMaterialTheme {
      UserProfileAddMfaBottomSheetContent(
        mfaPhoneCodeIsEnabled = false,
        mfaAuthenticatorAppIsEnabled = true,
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewBottomSheetAuthAppDisabled() {
  val backStack = rememberNavBackStack(UserProfileDestination.UserProfileSecurity)
  UserProfileStateProvider(backStack) {
    ClerkMaterialTheme {
      UserProfileAddMfaBottomSheetContent(
        mfaPhoneCodeIsEnabled = true,
        mfaAuthenticatorAppIsEnabled = false,
      )
    }
  }
}
