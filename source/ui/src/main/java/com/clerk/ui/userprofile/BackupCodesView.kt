package com.clerk.ui.userprofile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun BackupCodesView(
  mfaType: MfaType,
  codes: ImmutableList<String>,
  modifier: Modifier = Modifier,
) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(dp24)
          .then(modifier),
      verticalArrangement = Arrangement.spacedBy(dp24, alignment = Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = mfaType.instructions(),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )

      Surface(
        color = ClerkMaterialTheme.colors.background,
        modifier = Modifier.fillMaxWidth(),
        shape = ClerkMaterialTheme.shape,
        border = BorderStroke(width = dp1, color = ClerkMaterialTheme.computedColors.inputBorder),
      ) {
        BackupCodeGrid(codes = codes)
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dp24, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ClerkButton(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.download),
          onClick = {},
          configuration = ClerkButtonConfiguration(ClerkButtonConfiguration.ButtonStyle.Secondary),
        )
        ClerkButton(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.copy_to_clipboard),
          onClick = {},
          configuration = ClerkButtonConfiguration(ClerkButtonConfiguration.ButtonStyle.Secondary),
        )
      }
    }
  }
}

@Composable
fun BackupCodeGrid(codes: ImmutableList<String>, modifier: Modifier = Modifier) {
  LazyVerticalGrid(
    modifier = Modifier.fillMaxWidth().padding(horizontal = dp24).then(modifier),
    columns = GridCells.Fixed(count = 2),
  ) {
    items(codes) {
      Text(
        text = it,
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.foreground,
        modifier = Modifier.fillMaxWidth().padding(dp24),
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun MfaType.instructions() =
  when (this) {
    MfaType.PhoneCode ->
      stringResource(R.string.when_signing_in_you_will_need_to_enter_a_verification_code)
    MfaType.AuthenticatorApp -> stringResource(R.string.two_step_verification_is_now_enabled)
    MfaType.BackupCodes -> stringResource(R.string.backup_codes_are_now_enabled)
  }

internal enum class MfaType {
  PhoneCode,
  AuthenticatorApp,
  BackupCodes,
}

@PreviewLightDark
@Composable
private fun Preview() {
  BackupCodesView(
    codes =
      listOf(
          "jsdwz752",
          "abxkq983",
          "abxkq983",
          "mpltk294",
          "mpltk294",
          "qwert678",
          "dj2b5ugx",
          "xyztj501",
          "qwert678",
          "4nb52vql",
        )
        .toImmutableList(),
    mfaType = MfaType.AuthenticatorApp,
  )
}
