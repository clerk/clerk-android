package com.clerk.ui.userprofile.totp

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.network.model.totp.TOTPResource
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.PreviewUserProfileStateProvider
import com.clerk.ui.userprofile.common.BottomSheetTopBar
import com.clerk.ui.userprofile.verify.Mode
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
 * @param modifier The [Modifier] to be applied to the layout.
 */
@Composable
internal fun UserProfileMfaAddTotpView(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  onVerify: (Mode) -> Unit,
) {
  UserProfileMfaAddTotpViewImpl(modifier = modifier, onDismiss = onDismiss, onVerify = onVerify)
}

@Composable
internal fun UserProfileMfaAddTotpViewImpl(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: UserProfileMfaTotpViewModel = viewModel(),
  previewState: UserProfileMfaTotpViewModel.State? = null,
  onVerify: (Mode) -> Unit,
) {

  val collectedState by viewModel.state.collectAsStateWithLifecycle()
  val state = previewState ?: collectedState
  BottomSheetTopBar(
    title = stringResource(R.string.add_authenticator_application),
    onClosePressed = onDismiss,
  )
  if (state is UserProfileMfaTotpViewModel.State.Loading) {
    Box(
      modifier = Modifier.fillMaxWidth().background(color = ClerkMaterialTheme.colors.background),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator(color = ClerkMaterialTheme.colors.foreground)
    }
  } else {
    UserProfileMfaAddTotpContent(modifier = modifier, state = state, onVerify = onVerify)
  }
}

@Composable
private fun UserProfileMfaAddTotpContent(
  state: UserProfileMfaTotpViewModel.State,
  modifier: Modifier = Modifier,
  onVerify: (Mode) -> Unit,
) {
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()
  Column(
    modifier =
      Modifier.fillMaxWidth().padding(horizontal = dp24).padding(vertical = dp24).then(modifier)
  ) {
    if (state is UserProfileMfaTotpViewModel.State.Success) {
      Text(
        text = stringResource(R.string.set_up_a_new_sign_in_method),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      DisplayTextWithActionButton(
        text = state.totpResource.secret!!,
        onClick = {
          scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", state.totpResource.secret)))
          }
        },
      )
      Text(
        text = stringResource(R.string.alternatively_if_your_authenticator_supports_totp),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      DisplayTextWithActionButton(
        text = state.totpResource.uri!!,
        onClick = {
          scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", state.totpResource.uri)))
          }
        },
      )
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.continue_text),
        onClick = { onVerify(Mode.Totp) },
      )
      Spacers.Vertical.Spacer24()
    }
  }
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
private fun CopyToClipboardButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
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
  val fakeResource =
    TOTPResource(
      id = "totp_123",
      secret = "JBSWY3DPEHPK3PXP",
      uri =
        "otpauth://totp/Example:User?secret=JBSWY3DPEHPK3PXP&issuer=Example&algorithm=SHA1&digits=6&period=30",
      verified = false,
      backupCodes = emptyList(),
      createdAt = 1_700_000_000_000,
      updatedAt = 1_700_000_000_000,
    )
  PreviewUserProfileStateProvider {
    ClerkMaterialTheme(clerkTheme = ClerkTheme(colors = DefaultColors.clerk)) {
      UserProfileMfaAddTotpViewImpl(
        previewState = UserProfileMfaTotpViewModel.State.Success(totpResource = fakeResource),
        onDismiss = {},
        onVerify = {},
      )
    }
  }
}
