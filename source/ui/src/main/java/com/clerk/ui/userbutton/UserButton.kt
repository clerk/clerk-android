package com.clerk.ui.userbutton

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
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
import com.clerk.api.session.Session
import com.clerk.api.session.requiresForcedMfa
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.user.User
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.R
import com.clerk.ui.auth.AuthView
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.core.composition.TelemetryProvider
import com.clerk.ui.core.dimens.dp36
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import com.clerk.ui.userprofile.UserProfileView
import com.clerk.ui.userprofile.custom.UserProfileCustomRow

/**
 * Self-contained avatar button + user profile flow.
 *
 * Drop this into a TopAppBar actions slot; on tap it will open the full user profile UI in a
 * full-screen dialog and close itself when done.
 *
 * @param clerkTheme Optional theme customization for the user profile UI.
 * @param treatPendingAsSignedOut When `true`, the button will only appear when the session status
 *   is ACTIVE. When `false` (default), the button may appear in pending sessions.
 * @param routeToAuthWhenForcedMfa When `true` (default), clicking the button while the current
 *   session has unresolved MFA setup tasks routes to auth instead of opening profile.
 * @param customRows Custom rows to display on the profile account screen.
 * @param customDestination Composable that renders the destination for a given route key. The
 *   route key matches [UserProfileCustomRow.routeKey] of the tapped row. Custom destinations
 *   survive activity recreation (e.g. rotation).
 * @param onRequiresForcedMfaClick Optional callback used when the current session has outstanding
 *   MFA setup tasks. If not provided, the button will open [AuthView] in a full-screen dialog.
 */
@SuppressLint("LocalContextGetResourceValueCall", "ComposeModifierMissing")
@Composable
fun UserButton(
  clerkTheme: ClerkTheme? = null,
  treatPendingAsSignedOut: Boolean = false,
  routeToAuthWhenForcedMfa: Boolean = true,
  customRows: List<UserProfileCustomRow> = emptyList(),
  customDestination: (@Composable (String) -> Unit)? = null,
  onRequiresForcedMfaClick: (() -> Unit)? = null,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    TelemetryProvider {
      val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
      val sessionUser by Clerk.userFlow.collectAsStateWithLifecycle()
      val effectiveSession = session ?: Clerk.session
      val resolved =
        resolveUserButtonState(
          sessionExists = effectiveSession != null,
          sessionUser = sessionUser ?: effectiveSession?.user,
          activeUser =
            effectiveSession?.takeIf { it.status == Session.SessionStatus.ACTIVE }?.user
              ?: Clerk.activeUser
              ?: Clerk.user,
          treatPendingAsSignedOut = treatPendingAsSignedOut,
        )
      val requiresForcedMfa = effectiveSession?.requiresForcedMfa == true
      val user = resolved.user
      val shouldShowButton = resolved.shouldShowButton
      val telemetry = LocalTelemetryCollector.current
      var showProfile by rememberSaveable { mutableStateOf(false) }
      var showAuth by rememberSaveable { mutableStateOf(false) }

      LaunchedEffect(shouldShowButton, user?.id) {
        if (shouldShowButton) telemetry.record(TelemetryEvents.viewDidAppear("UserButton"))
      }

      LaunchedEffect(requiresForcedMfa, showAuth) {
        if (!requiresForcedMfa && showAuth) {
          showAuth = false
        }
      }

      if (shouldShowButton) {
        UserButtonContent(
          imageUrl = user?.imageUrl,
          onClick = {
            handleUserButtonClick(
              requiresForcedMfa = requiresForcedMfa,
              routeToAuthWhenForcedMfa = routeToAuthWhenForcedMfa,
              onRequiresForcedMfaClick = onRequiresForcedMfaClick,
              onOpenProfile = { showProfile = true },
              onOpenAuth = { showAuth = true },
            )
          },
        )
        if (showProfile) {
          UserProfileDialog(
            onDismiss = { showProfile = false },
            customRows = customRows,
            customDestination = customDestination,
          )
        }
        if (showAuth) {
          AuthDialog(onDismiss = { showAuth = false })
        }
      }
    }
  }
}

private data class ResolvedUserButtonState(val user: User?, val shouldShowButton: Boolean)

private fun resolveUserButtonState(
  sessionExists: Boolean,
  sessionUser: User?,
  activeUser: User?,
  treatPendingAsSignedOut: Boolean,
): ResolvedUserButtonState {
  val user =
    if (treatPendingAsSignedOut) {
      activeUser
    } else {
      sessionUser ?: activeUser
    }

  val shouldShowButton =
    shouldShowUserButton(
      hasSession = sessionExists,
      hasActiveUser = activeUser != null,
      treatPendingAsSignedOut = treatPendingAsSignedOut,
    )

  return ResolvedUserButtonState(user = user, shouldShowButton = shouldShowButton)
}

private fun handleUserButtonClick(
  requiresForcedMfa: Boolean,
  routeToAuthWhenForcedMfa: Boolean,
  onRequiresForcedMfaClick: (() -> Unit)?,
  onOpenProfile: () -> Unit,
  onOpenAuth: () -> Unit,
) {
  when (
    userButtonClickAction(
      requiresForcedMfa = requiresForcedMfa,
      routeToAuthWhenForcedMfa = routeToAuthWhenForcedMfa,
    )
  ) {
    UserButtonClickAction.OPEN_PROFILE -> onOpenProfile()
    UserButtonClickAction.ROUTE_TO_AUTH -> onRequiresForcedMfaClick?.invoke() ?: onOpenAuth()
  }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun UserButtonContent(imageUrl: String?, onClick: () -> Unit) {
  val context = LocalContext.current
  val profilePainter = painterResource(id = R.drawable.ic_profile)
  IconButton(onClick = onClick) {
    Box(
      modifier =
        Modifier.size(dp36).clip(CircleShape).semantics {
          contentDescription = context.getString(R.string.open_user_profile)
        },
      contentAlignment = Alignment.Center,
    ) {
      if (imageUrl.isNullOrBlank()) {
        Icon(
          painter = profilePainter,
          contentDescription = stringResource(R.string.user_avatar),
          modifier = Modifier.matchParentSize(),
          tint = Color.Unspecified,
        )
      } else {
        val model =
          ImageRequest.Builder(LocalContext.current).data(imageUrl).crossfade(true).build()
        AsyncImage(
          modifier = Modifier.matchParentSize().clip(CircleShape),
          model = model,
          contentDescription = stringResource(R.string.user_avatar),
          contentScale = ContentScale.Crop,
          placeholder = profilePainter,
          fallback = profilePainter,
          error = profilePainter,
        )
      }
    }
  }
}

@Composable
private fun UserProfileDialog(
  onDismiss: () -> Unit,
  customRows: List<UserProfileCustomRow> = emptyList(),
  customDestination: (@Composable (String) -> Unit)? = null,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    UserProfileView(
      onDismiss = onDismiss,
      customRows = customRows,
      customDestination = customDestination,
    )
  }
}

@Composable
private fun AuthDialog(onDismiss: () -> Unit) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    AuthView(modifier = Modifier.fillMaxSize(), onAuthComplete = onDismiss)
  }
}

internal enum class UserButtonClickAction {
  OPEN_PROFILE,
  ROUTE_TO_AUTH,
}

internal fun userButtonClickAction(
  requiresForcedMfa: Boolean,
  routeToAuthWhenForcedMfa: Boolean,
): UserButtonClickAction {
  return if (requiresForcedMfa && routeToAuthWhenForcedMfa) {
    UserButtonClickAction.ROUTE_TO_AUTH
  } else {
    UserButtonClickAction.OPEN_PROFILE
  }
}

internal fun shouldShowUserButton(
  hasSession: Boolean,
  hasActiveUser: Boolean,
  treatPendingAsSignedOut: Boolean,
): Boolean {
  return if (treatPendingAsSignedOut) {
    hasActiveUser
  } else {
    hasSession
  }
}
