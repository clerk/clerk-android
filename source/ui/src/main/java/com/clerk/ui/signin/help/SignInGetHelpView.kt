package com.clerk.ui.signin.help

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.log.ClerkLog
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.coroutines.launch

@Composable
internal fun SignInGetHelpView(modifier: Modifier = Modifier) {
  val authState = LocalAuthState.current
  val snackbarHostState = remember { SnackbarHostState() }
  ClerkThemedAuthScaffold(
    snackbarHostState = snackbarHostState,
    modifier = modifier,
    title = stringResource(R.string.get_help),
    subtitle = stringResource(R.string.if_you_have_trouble_signing_into_your_account),
    onBackPressed = { authState.navigateBack() },
  ) {
    val defaultSupportEmail = stringResource(R.string.support_clerk_com)
    val supportEmail = Clerk.supportEmail ?: defaultSupportEmail
    val emailSupport = rememberEmailSupport(snackbarHostState)

    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.email_support),
      onClick = { emailSupport(supportEmail) },
    )
  }
}

internal fun supportEmailIntent(supportEmail: String): Intent =
  Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", supportEmail, null))

@Composable
internal fun rememberEmailSupport(snackbarHostState: SnackbarHostState): (String) -> Unit {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val noEmailClientsMessage = stringResource(R.string.no_email_clients_installed_on_device)
  val chooserTitle = stringResource(R.string.email_support)
  return remember(context, scope, snackbarHostState, noEmailClientsMessage, chooserTitle) {
    { supportEmail ->
      try {
        context.startActivity(Intent.createChooser(supportEmailIntent(supportEmail), chooserTitle))
      } catch (_: ActivityNotFoundException) {
        ClerkLog.e("No email clients installed on device.")
        scope.launch {
          snackbarHostState.showSnackbar(
            message = noEmailClientsMessage,
            duration = SnackbarDuration.Short,
          )
        }
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInGetHelpView() {
  PreviewAuthStateProvider { ClerkMaterialTheme { SignInGetHelpView() } }
}
