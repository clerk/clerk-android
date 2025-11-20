package com.clerk.ui.userbutton

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.clerk.api.Clerk
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
 */
@Composable
fun UserButton(modifier: Modifier = Modifier, size: Dp = dp36, clerkTheme: ClerkTheme? = null) {
  ClerkThemeOverrideProvider(clerkTheme) {
    TelemetryProvider {
      val user by Clerk.userFlow.collectAsStateWithLifecycle()
      val context = LocalContext.current
      val telemetry = LocalTelemetryCollector.current

      var showProfile by rememberSaveable { mutableStateOf(false) }

      LaunchedEffect(user?.id) {
        if (user != null) {
          telemetry.record(TelemetryEvents.viewDidAppear("UserButton"))
        }
      }
      if (user != null) {
        IconButton(onClick = { showProfile = true }) {
          Box(
            modifier =
              modifier.size(size).clip(CircleShape).semantics {
                contentDescription = context.getString(R.string.open_user_profile)
              },
            contentAlignment = Alignment.Center,
          ) {
            val model =
              ImageRequest.Builder(LocalContext.current)
                .data(user?.imageUrl)
                .crossfade(true)
                .build()

            AsyncImage(
              modifier = Modifier.matchParentSize().clip(CircleShape),
              model = model,
              contentDescription = stringResource(R.string.user_avatar),
              contentScale = ContentScale.Crop,
              fallback = painterResource(id = R.drawable.ic_profile),
              onError = { /* fall through to placeholder below */ },
            )

            if (user?.imageUrl?.isBlank() == true) {
              Icon(
                painter = painterResource(id = R.drawable.ic_profile),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
              )
            }
          }
        }

        if (showProfile) {
          Dialog(
            onDismissRequest = { showProfile = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
          ) {
            UserProfileView(modifier = Modifier, onDismiss = { showProfile = false })
          }
        }
      }
    }
  }
}
