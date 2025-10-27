package com.clerk.ui.userprofile.totp

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.network.model.totp.TOTPResource
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import kotlinx.coroutines.launch

/**
 * A Composable that provides a user interface for adding a new Time-based One-Time Password (TOTP)
 * method for multi-factor authentication. It displays the secret key and a URI that can be used
 * with an authenticator app. Users can copy these values to the clipboard.
 *
 * This view is typically shown after the user has initiated the process of adding an authenticator
 * app as a second factor. It provides the necessary information for the user to configure their
 * app.
 *
 * @param totpResource The [TOTPResource] containing the secret and URI for setting up the
 *   authenticator.
 * @param modifier The [Modifier] to be applied to the layout.
 */
@Composable
fun UserProfileMfaAddTotpView(totpResource: TOTPResource, modifier: Modifier = Modifier) {
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()
  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = stringResource(R.string.add_authenticator_application),
    onBackPressed = {},
    content = {
      Text(
        text = stringResource(R.string.set_up_a_new_sign_in_method),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      DisplayTextWithActionButton(
        text = totpResource.secret,
        onClick = {
          scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", totpResource.secret)))
          }
        },
      )
      Text(
        text = stringResource(R.string.alternatively_if_your_authenticator_supports_totp),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      DisplayTextWithActionButton(
        text = totpResource.uri,
        onClick = {
          scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", totpResource.uri)))
          }
        },
      )
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.continue_text),
        onClick = {},
      )
      Spacers.Vertical.Spacer24()
      ClerkTextButton(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = stringResource(R.string.scan_qr_code_instead),
        onClick = {},
        textStyle = ClerkMaterialTheme.typography.bodyMedium,
      )
    },
  )
}

@Composable
private fun DisplayTextWithActionButton(text: String, onClick: () -> Unit) {
  Column {
    Spacers.Vertical.Spacer24()
    TextDisplayBox(text = text)
    Spacers.Vertical.Spacer12()
    CopyToClipboardButton(onClick = onClick)
    Spacers.Vertical.Spacer24()
  }
}

@Composable
fun CopyToClipboardButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
  ClerkButton(
    modifier = Modifier.fillMaxWidth().then(modifier),
    text = stringResource(R.string.copy_to_clipboard),
    onClick = onClick,
    icons =
      ClerkButtonDefaults.icons(
        leadingIcon = R.drawable.ic_clipboard,
        leadingIconColor = ClerkMaterialTheme.colors.mutedForeground,
      ),
    configuration =
      ClerkButtonDefaults.configuration(
        style = ClerkButtonConfiguration.ButtonStyle.Secondary,
        emphasis = ClerkButtonConfiguration.Emphasis.High,
      ),
  )
}

@Composable
private fun TextDisplayBox(text: String, modifier: Modifier = Modifier) {
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .background(color = ClerkMaterialTheme.colors.muted, shape = ClerkMaterialTheme.shape)
        .border(
          dp1,
          color = ClerkMaterialTheme.computedColors.inputBorder,
          shape = ClerkMaterialTheme.shape,
        )
        .padding(vertical = dp18, horizontal = dp18)
        .then(modifier)
  ) {
    Text(
      modifier = Modifier.align(Alignment.Center),
      text = text,
      style = ClerkMaterialTheme.typography.bodyMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme {
    UserProfileMfaAddTotpView(
      totpResource =
        TOTPResource(
          id = "1",
          secret = "MATT2JN8YFCTF7BUC6Z2BUAUI3HKOSRC",
          uri =
            "otpauth://totp/Clerk:georgevanjek@clerk.dev?algorithm=SHA1&digits=6" +
              "&issuer=Clerk&period=30&secret=MATT2JN8YFCTF7BUC6Z2BUAUI3HKOSRC",
          verified = true,
          createdAt = 1L,
          updatedAt = 1L,
        )
    )
  }
}
