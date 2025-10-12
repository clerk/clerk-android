package com.clerk.ui.userprofile.security.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileMfaRow(style: Style, modifier: Modifier = Modifier) {
  ClerkMaterialTheme {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp24)
          .padding(vertical = dp16)
          .then(modifier),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        modifier = Modifier.size(dp18),
        tint = ClerkMaterialTheme.colors.mutedForeground,
        painter = painterResource(style.icon()),
        contentDescription = null,
      )
      Column {}
      Spacer(modifier = Modifier.weight(1f))
      ItemMoreMenu(
        dropDownItems =
          persistentListOf(
            DropDownItem(
              id = MfaAction.Remove,
              textRes = R.string.remove,
              danger = true,
              isHidden = style == Style.BackupCodes,
            ),
            DropDownItem(
              id = MfaAction.SetAsDefault,
              textRes = R.string.set_as_default,
              isHidden =
                style !is Style.Sms ||
                  Clerk.user?.totpEnabled != true && !style.phoneNumber.defaultSecondFactor,
            ),
            DropDownItem(
              id = MfaAction.Regenerate,
              textRes = R.string.regenerate,
              isHidden = style != Style.BackupCodes,
            ),
          ),
        onClick = {
          when (it) {
            MfaAction.Remove -> {
              if (style == Style.AuthenticatorApp) {}

              if (style is Style.Sms) {}
            }
            MfaAction.SetAsDefault -> TODO()
            MfaAction.Regenerate -> TODO()
          }
        },
      )
    }
  }
}

enum class MfaAction {
  Remove,
  SetAsDefault,
  Regenerate,
}

@Composable
fun Style.icon(): Int {
  return when (this) {
    Style.AuthenticatorApp -> R.drawable.ic_key
    is Style.Sms -> R.drawable.ic_phone
    Style.BackupCodes -> R.drawable.ic_lock
  }
}

sealed interface Style {
  data object AuthenticatorApp : Style

  data class Sms(val phoneNumber: PhoneNumber) : Style

  data object BackupCodes : Style
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme { Column { UserProfileMfaRow(style = Style.AuthenticatorApp) } }
}
