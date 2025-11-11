@file:Suppress("UnusedImport")

package com.clerk.ui.userbutton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp36

/**
 * A circular avatar button that shows only when [user] is non-null. Tapping opens a sheet with
 * [UserProfileSheetContent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserButton(
  modifier: Modifier = Modifier,
  size: Dp = dp36,
  onViewDidAppearOnce: (() -> Unit)? = null,
  onProfileOpened: (() -> Unit)? = null,
  profileContent: @Composable () -> Unit = { DefaultUserProfileSheetContent() },
  placeholderIconRes: Int? = null,
) {
  var sheetOpen by remember { mutableStateOf(false) }
  val currentOnAppear by rememberUpdatedState(onViewDidAppearOnce)
  val currentOnOpened by rememberUpdatedState(onProfileOpened)

  LaunchedEffect(Unit) { currentOnAppear?.invoke() }
  val user by Clerk.userFlow.collectAsStateWithLifecycle()

  LaunchedEffect(user?.id) { if (user == null) sheetOpen = false }

  if (user != null) {
    IconButton(
      onClick = {
        sheetOpen = true
        currentOnOpened?.invoke()
      }
    ) {
      Box(
        modifier =
          modifier.size(size).clip(CircleShape).semantics {
            contentDescription = "Open user profile"
          },
        contentAlignment = Alignment.Center,
      ) {
        val model =
          ImageRequest.Builder(LocalContext.current).data(user?.imageUrl).crossfade(true).build()

        AsyncImage(
          modifier = Modifier.matchParentSize().clip(CircleShape),
          model = model,
          contentDescription = "User avatar",
          contentScale = ContentScale.Crop,
          onError = { /* fall through to placeholder below */ },
        )

        if (user?.imageUrl?.isBlank() == true && placeholderIconRes != null) {
          Icon(
            painter = painterResource(id = R.drawable.ic_profile),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
          )
        }
      }
    }
  }

  if (sheetOpen) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    UserProfileSheet(
      sheetState = sheetState,
      onDismiss = { sheetOpen = false },
      content = profileContent,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileSheet(
  sheetState: SheetState,
  onDismiss: () -> Unit,
  content: @Composable () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) { content() }
}

/** Replace with your real profile UI */
@Composable
private fun DefaultUserProfileSheetContent() {
  Surface {
    Text(
      modifier = Modifier.background(MaterialTheme.colorScheme.surface).padding(dp24),
      text = "User Profile",
      style = MaterialTheme.typography.titleLarge,
    )
  }
}
