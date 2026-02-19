package com.clerk.ui.userbutton

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.clerk.api.Clerk
import com.clerk.api.session.requiresForcedMfa
import com.clerk.api.ui.ClerkTheme
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.R
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.core.composition.TelemetryProvider
import com.clerk.ui.core.dimens.dp36
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import com.clerk.ui.userprofile.UserProfileView

/**
 * Self-contained avatar button + user profile flow.
 *
 * Drop this into a TopAppBar actions slot; on tap it will open the full user profile UI in a
 * full-screen dialog and close itself when done.
 *
 * @param clerkTheme Optional theme customization for the user profile UI.
 * @param treatPendingAsSignedOut When `true`, the button will only appear when the session status
 *   is ACTIVE. When `false` (default), the button may appear in pending sessions except when the
 *   session requires forced MFA setup.
 */
@SuppressLint("LocalContextGetResourceValueCall", "ComposeModifierMissing")
@Composable
fun UserButton(clerkTheme: ClerkTheme? = null, treatPendingAsSignedOut: Boolean = false) {
  ClerkThemeOverrideProvider(clerkTheme) {
    TelemetryProvider {
      val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
      val sessionUser by Clerk.userFlow.collectAsStateWithLifecycle()
      val user =
        when {
          session?.requiresForcedMfa == true -> null
          treatPendingAsSignedOut -> Clerk.activeUser
          else -> sessionUser
        }
      val telemetry = LocalTelemetryCollector.current
      var showProfile by rememberSaveable { mutableStateOf(false) }

      LaunchedEffect(user?.id) {
        if (user != null) telemetry.record(TelemetryEvents.viewDidAppear("UserButton"))
      }

      if (user != null) {
        UserButtonContent(imageUrl = user.imageUrl, onClick = { showProfile = true })
        if (showProfile) {
          UserProfileDialog(onDismiss = { showProfile = false })
        }
      }
    }
  }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun UserButtonContent(imageUrl: String?, onClick: () -> Unit) {
  val context = LocalContext.current
  IconButton(onClick = onClick) {
    Box(
      modifier =
        Modifier.size(dp36).clip(CircleShape).semantics {
          contentDescription = context.getString(R.string.open_user_profile)
        },
      contentAlignment = Alignment.Center,
    ) {
      val model = ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build()
      AsyncImage(
        modifier = Modifier.matchParentSize().clip(CircleShape),
        model = model,
        contentDescription = stringResource(R.string.user_avatar),
        contentScale = ContentScale.Crop,
        fallback = painterResource(id = R.drawable.ic_profile),
        onError = { /* fall through to placeholder below */ },
      )
      if (imageUrl?.isBlank() == true) {
        Icon(painterResource(id = R.drawable.ic_profile), null, Modifier.matchParentSize())
      }
    }
  }
}

@Composable
private fun UserProfileDialog(onDismiss: () -> Unit) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    UserProfileView(onDismiss = onDismiss)
  }
}
