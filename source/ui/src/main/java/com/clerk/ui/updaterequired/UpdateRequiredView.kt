package com.clerk.ui.updaterequired

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.appversion.AppVersionSupportStatus
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.clerkTestTagsAsResourceIds
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.composition.ClerkLogoProvider
import com.clerk.ui.core.composition.LocalClerkLogoContent
import com.clerk.ui.core.log.ClerkLog
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.signin.help.supportEmailIntent
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import kotlinx.coroutines.launch

/**
 * A full-screen view telling the user to update the app because its `versionCode` is older than the
 * minimum set in the Clerk Dashboard.
 *
 * Clerk shows this view over your app automatically. To place it yourself, call
 * [com.clerk.api.ClerkConfigurationOptions.withUpdateRequiredViewDisabled] and show it while
 * [Clerk.appVersionSupportStatus] is unsupported:
 * ```kotlin
 * val status by Clerk.appVersionSupportStatus.collectAsStateWithLifecycle()
 * if (!status.isSupported) {
 *   UpdateRequiredView()
 * }
 * ```
 *
 * @param modifier The [Modifier] to apply to the view.
 * @param clerkTheme Overrides the theme passed to [Clerk.initialize] for this view.
 * @param logo Replaces the logo. When omitted, the view shows the logo set in the Clerk Dashboard,
 *   or your app's icon when there's none. The SDK applies no sizing or spacing to it.
 */
@Composable
public fun UpdateRequiredView(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  logo: (@Composable () -> Unit)? = null,
) {
  val status by Clerk.appVersionSupportStatus.collectAsStateWithLifecycle()
  val organizationLogoUrl by Clerk.organizationLogoUrlFlow.collectAsStateWithLifecycle()
  val customLogo = logo ?: LocalClerkLogoContent.current
  val resolvedLogo =
    customLogo ?: if (organizationLogoUrl.isNullOrBlank()) ({ AppIconLogo() }) else null

  ClerkThemeOverrideProvider(clerkTheme) {
    ClerkLogoProvider(resolvedLogo) {
      UpdateRequiredContent(
        status = status,
        appName = Clerk.applicationName ?: appLabel(LocalContext.current),
        supportEmail = Clerk.supportEmail,
        modifier = modifier,
      )
    }
  }
}

@Composable
internal fun UpdateRequiredContent(
  status: AppVersionSupportStatus,
  appName: String,
  supportEmail: String?,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val noEmailClientsMessage = stringResource(R.string.no_email_clients_installed_on_device)
  val emailSupportTitle = stringResource(R.string.email_support)

  ClerkThemedAuthScaffold(
    modifier = Modifier.fillMaxSize().clerkTestTagsAsResourceIds().then(modifier),
    title = stringResource(R.string.update_required),
    subtitle = stringResource(R.string.this_version_of_app_is_no_longer_supported, appName),
    snackbarHostState = snackbarHostState,
    hasBackButton = false,
    showSignedInUserButton = false,
  ) {
    status.updateUrl?.let { updateUrl ->
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(storeButtonText(updateUrl)),
        onClick = { openUpdateUrl(context, updateUrl) },
      )
    }

    if (supportEmail != null) {
      Spacers.Vertical.Spacer12()
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.cant_update),
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
        ClerkTextButton(text = stringResource(R.string.get_help)) {
          try {
            context.startActivity(
              Intent.createChooser(supportEmailIntent(supportEmail), emailSupportTitle)
            )
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
  }
}

internal fun storeButtonText(updateUrl: String): Int =
  if (updateUrl.toUri().host.equals(GOOGLE_PLAY_HOST, ignoreCase = true)) {
    R.string.go_to_google_play
  } else {
    R.string.update_app
  }

private fun openUpdateUrl(context: Context, updateUrl: String) {
  try {
    context.startActivity(Intent(Intent.ACTION_VIEW, updateUrl.toUri()))
  } catch (_: ActivityNotFoundException) {
    ClerkLog.e("No app can open the update URL.")
  }
}

@Composable
private fun AppIconLogo() {
  val context = LocalContext.current
  val size = ClerkMaterialTheme.design.logoMaxHeight
  val sizePx = with(LocalDensity.current) { size.roundToPx() }
  val icon = remember(context, sizePx) { appIcon(context, sizePx) } ?: return
  Image(
    bitmap = icon,
    contentDescription = stringResource(R.string.logo),
    modifier = Modifier.size(size),
  )
}

private fun appIcon(context: Context, sizePx: Int): ImageBitmap? =
  sizePx
    .takeIf { it > 0 }
    ?.let {
      context.packageManager
        .getApplicationIcon(context.applicationInfo)
        .toBitmap(width = it, height = it)
        .asImageBitmap()
    }

private fun appLabel(context: Context): String =
  context.applicationInfo.loadLabel(context.packageManager).toString()

private const val GOOGLE_PLAY_HOST = "play.google.com"

@PreviewLightDark
@Composable
private fun PreviewUpdateRequiredContent() {
  ClerkMaterialTheme {
    UpdateRequiredContent(
      status =
        AppVersionSupportStatus(
          isSupported = false,
          currentVersion = 41,
          minimumVersion = 42,
          updateUrl = "https://play.google.com/store/apps/details?id=com.example",
        ),
      appName = "Acme",
      supportEmail = "support@example.com",
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewUpdateRequiredContentWithoutLinks() {
  ClerkMaterialTheme {
    UpdateRequiredContent(
      status = AppVersionSupportStatus(isSupported = false, currentVersion = 41),
      appName = "Acme",
      supportEmail = null,
    )
  }
}
