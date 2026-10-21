package com.clerk.ui.updaterequired

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.appversion.AppVersionSupportStatus
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.clerkTestTagsAsResourceIds
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.composition.LocalClerkLogoContent
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp40
import com.clerk.ui.core.dimens.dp64
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.header.HeaderTextView
import com.clerk.ui.core.header.HeaderType
import com.clerk.ui.core.log.ClerkLog
import com.clerk.ui.signin.help.rememberEmailSupport
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider

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
  val resolvedLogo = logo ?: LocalClerkLogoContent.current

  ClerkThemeOverrideProvider(clerkTheme) {
    UpdateRequiredContent(
      status = status,
      appName = Clerk.applicationName ?: appLabel(LocalContext.current),
      supportEmail = Clerk.supportEmail,
      modifier = modifier,
      logo = resolvedLogo ?: { InstanceOrAppLogo(organizationLogoUrl) },
    )
  }
}

@Composable
internal fun UpdateRequiredContent(
  status: AppVersionSupportStatus,
  appName: String,
  supportEmail: String?,
  modifier: Modifier = Modifier,
  logo: @Composable () -> Unit = { AppIconLogo() },
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val emailSupport = rememberEmailSupport(snackbarHostState)

  ClerkMaterialTheme {
    Scaffold(
      modifier = Modifier.fillMaxSize().clerkTestTagsAsResourceIds().then(modifier),
      containerColor = ClerkMaterialTheme.colors.background,
      snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
    ) { innerPadding ->
      Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = dp24),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(top = dp64),
          verticalArrangement = Arrangement.spacedBy(dp24),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          logo()
          Column(
            verticalArrangement = Arrangement.spacedBy(dp8),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            HeaderTextView(text = stringResource(R.string.update_required), type = HeaderType.Title)
            HeaderTextView(
              text = stringResource(R.string.this_version_of_app_is_no_longer_supported, appName),
              type = HeaderType.Subtitle,
            )
          }
          val currentVersion = status.currentVersion
          val minimumVersion = status.minimumVersion
          if (currentVersion != null && minimumVersion != null) {
            VersionComparison(currentVersion = currentVersion, minimumVersion = minimumVersion)
          }
        }

        Column(
          modifier =
            Modifier.widthIn(max = dp320).fillMaxWidth().padding(top = dp16, bottom = dp40),
          verticalArrangement = Arrangement.spacedBy(dp12),
        ) {
          status.updateUrl?.let { updateUrl ->
            ClerkButton(
              modifier = Modifier.fillMaxWidth(),
              text = stringResource(storeButtonText(updateUrl)),
              onClick = { openUpdateUrl(context, updateUrl) },
            )
          }
          if (supportEmail != null) {
            GetHelpButton(onClick = { emailSupport(supportEmail) })
          }
        }
      }
    }
  }
}

@Composable
private fun VersionComparison(currentVersion: Long, minimumVersion: Long) {
  val shape = RoundedCornerShape(dp12)
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(IntrinsicSize.Min)
        .clip(shape)
        .background(ClerkMaterialTheme.colors.neutral.copy(alpha = VERSION_BOX_BACKGROUND_ALPHA))
        .border(dp1, ClerkMaterialTheme.computedColors.border, shape)
        .padding(dp16),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    VersionColumn(
      label = stringResource(R.string.your_version),
      version = currentVersion.toString(),
      modifier = Modifier.weight(1f),
    )
    Box(
      modifier =
        Modifier.width(dp1).fillMaxHeight().background(ClerkMaterialTheme.computedColors.border)
    )
    VersionColumn(
      label = stringResource(R.string.required_version),
      version = "$minimumVersion+",
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun VersionColumn(label: String, version: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.semantics(mergeDescendants = true) {},
    verticalArrangement = Arrangement.spacedBy(dp2),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = label,
      style = ClerkMaterialTheme.typography.titleSmall,
      color = ClerkMaterialTheme.colors.mutedForeground,
      textAlign = TextAlign.Center,
    )
    Text(
      text = version,
      style = ClerkMaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      color = ClerkMaterialTheme.colors.foreground,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun GetHelpButton(onClick: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .height(dp32)
        .clip(ClerkMaterialTheme.shape)
        .clickable(role = Role.Button, onClick = onClick),
    horizontalArrangement = Arrangement.spacedBy(dp4, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.cant_update),
      style = ClerkMaterialTheme.typography.titleSmall,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    Text(
      text = stringResource(R.string.get_help),
      style = ClerkMaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
      color = ClerkMaterialTheme.colors.primary,
    )
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

private fun appLabel(context: Context): String =
  context.applicationInfo.loadLabel(context.packageManager).toString()

private const val GOOGLE_PLAY_HOST = "play.google.com"
private const val VERSION_BOX_BACKGROUND_ALPHA = 0.02f
private val dp320 = 320.dp

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
