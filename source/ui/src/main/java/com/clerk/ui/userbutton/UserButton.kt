@file:Suppress("UnusedImport")

package com.clerk.ui.userbutton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.clerk.api.Clerk
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.R
import com.clerk.ui.auth.LocalTelemetryCollector
import com.clerk.ui.auth.TelemetryProvider
import com.clerk.ui.core.dimens.dp36

/**
 * A circular button that displays the current user's avatar. The button is only visible when a user
 * is signed in. Tapping it triggers the `onClick` lambda, which is typically used to open a user
 * profile management interface.
 *
 * If the user has an image URL, it will be displayed. Otherwise, a default profile icon is shown.
 *
 * @param modifier The [Modifier] to be applied to the component.
 * @param size The size of the circular button. Defaults to `36.dp`.
 * @param onClick The lambda to be executed when the button is tapped.
 */
@Composable
fun UserButton(modifier: Modifier = Modifier, size: Dp = dp36, onClick: () -> Unit) {

  TelemetryProvider {
    val telemetryCollector = LocalTelemetryCollector.current
    val user by Clerk.userFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { telemetryCollector.record(TelemetryEvents.viewDidAppear("UserButton")) }

    if (user != null) {
      IconButton(onClick = { onClick() }) {
        Box(
          modifier =
            modifier.size(size).clip(CircleShape).semantics {
              contentDescription = context.getString(R.string.open_user_profile)
            },
          contentAlignment = Alignment.Center,
        ) {
          val model =
            ImageRequest.Builder(LocalContext.current).data(user?.imageUrl).crossfade(true).build()

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
    }
  }
}
