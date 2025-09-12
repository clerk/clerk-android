package com.clerk.ui.signin.help

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.core.net.toUri
import com.clerk.api.log.ClerkLog
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.common.ClerkAuthScaffold
import com.clerk.ui.core.common.SecuredByClerk
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.coroutines.launch

@Composable
fun SignInGetHelpView(modifier: Modifier = Modifier, onBackPressed: () -> Unit = {}) {
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  ClerkMaterialTheme {
    ClerkAuthScaffold(
      snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
      modifier = modifier,
      title = stringResource(R.string.get_help),
      subtitle = stringResource(R.string.if_you_have_trouble_signing_into_your_account),
      onBackPressed = onBackPressed,
    ) {
      val context = LocalContext.current
      val emailIntent =
        Intent(Intent.ACTION_SENDTO).apply {
          data = "mailto:".toUri()
          putExtra(Intent.EXTRA_EMAIL, arrayOf(stringResource(R.string.support_clerk_com)))
        }

      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.email_support),
        onClick = {
          try {
            context.startActivity(Intent.createChooser(emailIntent, "Contact Clerk Support"))
          } catch (_: ActivityNotFoundException) {
            ClerkLog.e("No email clients installed on device.")
            scope.launch {
              snackbarHostState.showSnackbar(
                message = context.getString(R.string.no_email_clients_installed_on_device),
                duration = SnackbarDuration.Short,
              )
            }
          }
        },
      )
      Spacers.Vertical.Spacer32()
      SecuredByClerk()
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInGetHelpView() {
  ClerkMaterialTheme() { SignInGetHelpView() }
}
