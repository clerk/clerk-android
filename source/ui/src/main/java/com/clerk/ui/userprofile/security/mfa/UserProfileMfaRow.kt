package com.clerk.ui.userprofile.security.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.R
import com.clerk.ui.core.badge.Badge
import com.clerk.ui.core.badge.ClerkBadgeType
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.util.formattedAsPhoneNumberIfPossible
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileMfaRow(
  style: Style,
  modifier: Modifier = Modifier,
  isDefault: Boolean = false,
  title: String? = null,
) {
  val hasHeader = isDefault || title != null

  ClerkMaterialTheme {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp24, vertical = dp16)
          .then(modifier),
      horizontalArrangement = Arrangement.spacedBy(dp16),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val padding = if (title != null) dp4 else dp0
      if (hasHeader) {
        Icon(
          painter = painterResource(style.icon()),
          tint = ClerkMaterialTheme.colors.mutedForeground,
          contentDescription = null,
          modifier = Modifier.padding(top = padding).size(dp18).align(Alignment.Top),
        )
      } else {
        Box(
          modifier = Modifier.size(dp18).align(Alignment.CenterVertically),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painter = painterResource(style.icon()),
            tint = ClerkMaterialTheme.colors.mutedForeground,
            contentDescription = null,
            modifier = Modifier.size(dp18),
          )
        }
      }

      Column(verticalArrangement = Arrangement.Center) {
        if (isDefault) {
          Badge(text = stringResource(R.string.default_text), badgeType = ClerkBadgeType.Secondary)
          Spacers.Vertical.Spacer4()
        }

        title?.let {
          Text(
            text = it,
            style = ClerkMaterialTheme.typography.bodyMedium,
            color = ClerkMaterialTheme.colors.mutedForeground,
          )
        }

        Text(
          text = style.text(),
          style = ClerkMaterialTheme.typography.bodyLarge,
          color = ClerkMaterialTheme.colors.foreground,
        )
      }

      Spacer(Modifier.weight(1f))
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
                  (Clerk.user?.totpEnabled != true && !style.phoneNumber.defaultSecondFactor),
            ),
            DropDownItem(
              id = MfaAction.Regenerate,
              textRes = R.string.regenerate,
              isHidden = style != Style.BackupCodes,
            ),
          ),
        onClick = { /* ... */ },
      )
    }
  }
}

@Composable
fun Style.text(): String {
  return when (this) {
    Style.AuthenticatorApp -> "Authenticator app"
    is Style.Sms -> "SMS code ${this.phoneNumber.phoneNumber.formattedAsPhoneNumberIfPossible}"
    Style.BackupCodes -> "Backup codes"
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

@Stable
sealed interface Style {
  data object AuthenticatorApp : Style

  data class Sms(val phoneNumber: PhoneNumber) : Style

  data object BackupCodes : Style
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Column {
      UserProfileMfaRow(style = Style.AuthenticatorApp, isDefault = true)
      UserProfileMfaRow(
        style = Style.Sms(phoneNumber = PhoneNumber(id = "1", phoneNumber = "+13012370655")),
        title = "Primary",
      )
      UserProfileMfaRow(style = Style.BackupCodes)
    }
  }
}
