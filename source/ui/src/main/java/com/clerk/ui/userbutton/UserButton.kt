@file:Suppress("TooManyFunctions")

package com.clerk.ui.userbutton

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.session.Session
import com.clerk.api.session.pendingTaskKey
import com.clerk.api.session.requiresForcedMfa
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.user.User
import com.clerk.api.user.fullName
import com.clerk.telemetry.TelemetryCollector
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.R
import com.clerk.ui.auth.AuthView
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.core.composition.TelemetryProvider
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp36
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import com.clerk.ui.userprofile.UserProfileView
import com.clerk.ui.userprofile.custom.UserProfileCustomRow
import kotlinx.coroutines.launch

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
 * @param customDestination Composable that renders the destination for a given route key. The route
 *   key matches [UserProfileCustomRow.routeKey] of the tapped row. Custom destinations survive
 *   activity recreation (e.g. rotation).
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
      UserButtonPresenter(
        treatPendingAsSignedOut = treatPendingAsSignedOut,
        routeToAuthWhenForcedMfa = routeToAuthWhenForcedMfa,
        customRows = customRows,
        customDestination = customDestination,
        onRequiresForcedMfaClick = onRequiresForcedMfaClick,
      )
    }
  }
}

private data class ResolvedUserButtonState(val user: User?, val shouldShowButton: Boolean)

@Composable
private fun UserButtonPresenter(
  treatPendingAsSignedOut: Boolean,
  routeToAuthWhenForcedMfa: Boolean,
  customRows: List<UserProfileCustomRow>,
  customDestination: (@Composable (String) -> Unit)?,
  onRequiresForcedMfaClick: (() -> Unit)?,
) {
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  val sessionUser by Clerk.userFlow.collectAsStateWithLifecycle()
  val effectiveSession = session ?: Clerk.session
  val resolved = resolvedUserButtonState(effectiveSession, sessionUser, treatPendingAsSignedOut)
  val requiresForcedMfa = effectiveSession?.requiresForcedMfa == true
  val hasPendingNonMfaTask = effectiveSession?.pendingTaskKey != null && !requiresForcedMfa
  val user = resolved.user
  val telemetry = LocalTelemetryCollector.current
  var showProfile by rememberSaveable { mutableStateOf(false) }
  var showPendingSessionSheet by rememberSaveable { mutableStateOf(false) }
  var authMode by rememberSaveable { mutableStateOf<UserButtonAuthMode?>(null) }

  ObserveUserButtonState(
    shouldShowButton = resolved.shouldShowButton,
    user = user,
    hasPendingNonMfaTask = hasPendingNonMfaTask,
    requiresForcedMfa = requiresForcedMfa,
    authMode = authMode,
    telemetry = telemetry,
    onDismissPendingSessionSheet = { showPendingSessionSheet = false },
    onDismissAuth = { authMode = null },
  )

  if (resolved.shouldShowButton && user != null) {
    UserAvatarButtonContent(
      imageUrl = user.imageUrl,
      onClick = {
        val callbacks =
          UserButtonClickCallbacks(
            onRequiresForcedMfaClick = onRequiresForcedMfaClick,
            onOpenProfile = { showProfile = true },
            onOpenPendingSessionSheet = { showPendingSessionSheet = true },
            onOpenAuth = { authMode = UserButtonAuthMode.ForcedMfa },
          )
        handleUserButtonClick(
          requiresForcedMfa = requiresForcedMfa,
          hasPendingNonMfaTask = hasPendingNonMfaTask,
          routeToAuthWhenForcedMfa = routeToAuthWhenForcedMfa,
          callbacks = callbacks,
        )
      },
    )
  }

  UserButtonDialogHosts(
    showProfile = showProfile,
    showPendingSessionSheet = showPendingSessionSheet,
    user = user,
    session = effectiveSession,
    authMode = authMode,
    customRows = customRows,
    customDestination = customDestination,
    onDismissProfile = { showProfile = false },
    onDismissPendingSessionSheet = { showPendingSessionSheet = false },
    onDismissAuth = { authMode = null },
    onAddAccount = {
      showProfile = false
      authMode = UserButtonAuthMode.AddAccount
    },
  )
}

private fun resolvedUserButtonState(
  effectiveSession: Session?,
  sessionUser: User?,
  treatPendingAsSignedOut: Boolean,
): ResolvedUserButtonState {
  return resolveUserButtonState(
    sessionExists = effectiveSession != null,
    sessionUser = sessionUser ?: effectiveSession?.user,
    activeUser =
      effectiveSession?.takeIf { it.status == Session.SessionStatus.ACTIVE }?.user
        ?: Clerk.activeUser
        ?: Clerk.user,
    treatPendingAsSignedOut = treatPendingAsSignedOut,
  )
}

@Composable
@Suppress("LongParameterList")
private fun ObserveUserButtonState(
  shouldShowButton: Boolean,
  user: User?,
  hasPendingNonMfaTask: Boolean,
  requiresForcedMfa: Boolean,
  authMode: UserButtonAuthMode?,
  telemetry: TelemetryCollector,
  onDismissPendingSessionSheet: () -> Unit,
  onDismissAuth: () -> Unit,
) {
  LaunchedEffect(shouldShowButton, user?.id) {
    if (shouldShowButton) telemetry.record(TelemetryEvents.viewDidAppear("UserButton"))
  }
  LaunchedEffect(hasPendingNonMfaTask) { if (!hasPendingNonMfaTask) onDismissPendingSessionSheet() }
  DismissAuthWhenMfaResolved(
    requiresForcedMfa = requiresForcedMfa,
    authMode = authMode,
    onDismissAuth = onDismissAuth,
  )
}

@Composable
@Suppress("LongParameterList")
private fun UserButtonDialogHosts(
  showProfile: Boolean,
  showPendingSessionSheet: Boolean,
  user: User?,
  session: Session?,
  authMode: UserButtonAuthMode?,
  customRows: List<UserProfileCustomRow>,
  customDestination: (@Composable (String) -> Unit)?,
  onDismissProfile: () -> Unit,
  onDismissPendingSessionSheet: () -> Unit,
  onDismissAuth: () -> Unit,
  onAddAccount: () -> Unit,
) {
  UserProfileDialogHost(
    showProfile = showProfile,
    customRows = customRows,
    customDestination = customDestination,
    onDismissProfile = onDismissProfile,
    onAddAccount = onAddAccount,
  )
  PendingSessionAccountSheetHost(
    showSheet = showPendingSessionSheet,
    user = user,
    session = session,
    onDismissRequest = onDismissPendingSessionSheet,
  )
  AuthDialogHost(authMode = authMode, onDismissAuth = onDismissAuth)
}

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
  hasPendingNonMfaTask: Boolean,
  routeToAuthWhenForcedMfa: Boolean,
  callbacks: UserButtonClickCallbacks,
) {
  when (
    userButtonClickAction(
      requiresForcedMfa = requiresForcedMfa,
      hasPendingNonMfaTask = hasPendingNonMfaTask,
      routeToAuthWhenForcedMfa = routeToAuthWhenForcedMfa,
    )
  ) {
    UserButtonClickAction.OPEN_PROFILE -> callbacks.onOpenProfile()
    UserButtonClickAction.OPEN_PENDING_SESSION_SHEET -> callbacks.onOpenPendingSessionSheet()
    UserButtonClickAction.ROUTE_TO_AUTH ->
      callbacks.onRequiresForcedMfaClick?.invoke() ?: callbacks.onOpenAuth()
  }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
private fun UserAvatarButtonContent(imageUrl: String?, onClick: () -> Unit) {
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

private data class UserButtonClickCallbacks(
  val onRequiresForcedMfaClick: (() -> Unit)?,
  val onOpenProfile: () -> Unit,
  val onOpenPendingSessionSheet: () -> Unit,
  val onOpenAuth: () -> Unit,
)

@Composable
private fun DismissAuthWhenMfaResolved(
  requiresForcedMfa: Boolean,
  authMode: UserButtonAuthMode?,
  onDismissAuth: () -> Unit,
) {
  LaunchedEffect(requiresForcedMfa, authMode) {
    if (shouldDismissAuthWhenMfaResolved(authMode, requiresForcedMfa)) {
      onDismissAuth()
    }
  }
}

@Composable
private fun UserProfileDialogHost(
  showProfile: Boolean,
  customRows: List<UserProfileCustomRow>,
  customDestination: (@Composable (String) -> Unit)?,
  onDismissProfile: () -> Unit,
  onAddAccount: () -> Unit,
) {
  if (showProfile) {
    UserProfileDialog(
      onDismiss = onDismissProfile,
      onAddAccount = onAddAccount,
      customRows = customRows,
      customDestination = customDestination,
    )
  }
}

@Composable
private fun AuthDialogHost(authMode: UserButtonAuthMode?, onDismissAuth: () -> Unit) {
  if (authMode != null) {
    AuthDialog(
      preferGoogleOneTap = authMode.preferGoogleOneTap,
      startSocialOAuthAsSignUp = authMode.startSocialOAuthAsSignUp,
      onDismiss = onDismissAuth,
    )
  }
}

@Composable
private fun PendingSessionAccountSheetHost(
  showSheet: Boolean,
  user: User?,
  session: Session?,
  onDismissRequest: () -> Unit,
) {
  if (showSheet && user != null && session != null) {
    PendingSessionAccountSheet(user = user, session = session, onDismissRequest = onDismissRequest)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingSessionAccountSheet(user: User, session: Session, onDismissRequest: () -> Unit) {
  ClerkMaterialTheme {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isSigningOut by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val avatarModel =
      remember(user.imageUrl, context) {
        ImageRequest.Builder(context).data(user.imageUrl).crossfade(true).build()
      }

    ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      sheetState = sheetState,
      containerColor = ClerkMaterialTheme.colors.background,
      contentColor = ClerkMaterialTheme.colors.foreground,
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dp24, vertical = dp16)) {
        PendingSessionAccountHeader(
          user = user,
          displayIdentifier = session.displayIdentifier(user),
          avatarModel = avatarModel,
        )
        HorizontalDivider(
          modifier = Modifier.padding(top = dp16),
          color = ClerkMaterialTheme.computedColors.border,
        )
        PendingSessionSignOutRow(
          isLoading = isSigningOut,
          onClick = {
            if (isSigningOut) return@PendingSessionSignOutRow
            isSigningOut = true
            errorMessage = null
            scope.launch {
              when (val result = Clerk.auth.signOut(sessionId = session.id)) {
                is ClerkResult.Success -> {
                  sheetState.hide()
                  onDismissRequest()
                }
                is ClerkResult.Failure -> {
                  isSigningOut = false
                  errorMessage = result.errorMessage
                }
              }
            }
          },
        )
        errorMessage?.let {
          Text(
            modifier = Modifier.padding(bottom = dp12),
            text = it,
            style = ClerkMaterialTheme.typography.bodyMedium,
            color = ClerkMaterialTheme.colors.danger,
          )
        }
      }
    }
  }
}

@Composable
private fun PendingSessionAccountHeader(
  user: User,
  displayIdentifier: String?,
  avatarModel: ImageRequest,
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    AsyncImage(
      modifier = Modifier.size(dp36).clip(CircleShape),
      model = avatarModel,
      contentDescription = stringResource(R.string.user_avatar),
      contentScale = ContentScale.Crop,
      placeholder = painterResource(id = R.drawable.ic_profile),
      fallback = painterResource(id = R.drawable.ic_profile),
      error = painterResource(id = R.drawable.ic_profile),
    )
    Column(modifier = Modifier.weight(1f).padding(start = dp12)) {
      val displayName = user.displayName()
      if (displayName.isNotBlank()) {
        Text(
          text = displayName,
          style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
      }
      displayIdentifier?.let {
        Text(
          text = it,
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
    }
  }
}

@Composable
private fun PendingSessionSignOutRow(isLoading: Boolean, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(enabled = !isLoading, onClick = onClick)
        .padding(vertical = dp16),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      modifier = Modifier.size(dp20),
      painter = painterResource(id = R.drawable.ic_sign),
      contentDescription = null,
      tint = ClerkMaterialTheme.colors.mutedForeground,
    )
    Text(
      modifier = Modifier.weight(1f).padding(start = dp12),
      text = stringResource(R.string.sign_out),
      style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
      color = ClerkMaterialTheme.colors.foreground,
    )
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(dp20), strokeWidth = dp2)
    }
  }
}

@Composable
private fun UserProfileDialog(
  onDismiss: () -> Unit,
  onAddAccount: () -> Unit,
  customRows: List<UserProfileCustomRow> = emptyList(),
  customDestination: (@Composable (String) -> Unit)? = null,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    UserProfileView(
      onDismiss = onDismiss,
      onAddAccount = onAddAccount,
      customRows = customRows,
      customDestination = customDestination,
    )
  }
}

@Composable
private fun AuthDialog(
  preferGoogleOneTap: Boolean,
  startSocialOAuthAsSignUp: Boolean,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    AuthView(
      modifier = Modifier.fillMaxSize(),
      preferGoogleOneTap = preferGoogleOneTap,
      startSocialOAuthAsSignUp = startSocialOAuthAsSignUp,
      onDismiss = onDismiss,
      onAuthComplete = onDismiss,
    )
  }
}

internal enum class UserButtonClickAction {
  OPEN_PROFILE,
  OPEN_PENDING_SESSION_SHEET,
  ROUTE_TO_AUTH,
}

internal enum class UserButtonAuthMode(
  val preferGoogleOneTap: Boolean,
  val startSocialOAuthAsSignUp: Boolean,
) {
  AddAccount(preferGoogleOneTap = false, startSocialOAuthAsSignUp = false),
  ForcedMfa(preferGoogleOneTap = true, startSocialOAuthAsSignUp = false),
}

internal fun shouldDismissAuthWhenMfaResolved(
  authMode: UserButtonAuthMode?,
  requiresForcedMfa: Boolean,
): Boolean {
  return authMode == UserButtonAuthMode.ForcedMfa && !requiresForcedMfa
}

internal fun userButtonClickAction(
  requiresForcedMfa: Boolean,
  hasPendingNonMfaTask: Boolean,
  routeToAuthWhenForcedMfa: Boolean,
): UserButtonClickAction {
  return if (requiresForcedMfa && routeToAuthWhenForcedMfa) {
    UserButtonClickAction.ROUTE_TO_AUTH
  } else if (hasPendingNonMfaTask) {
    UserButtonClickAction.OPEN_PENDING_SESSION_SHEET
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

private fun User.displayName(): String = fullName().ifBlank { username.orEmpty() }

private fun Session.displayIdentifier(user: User): String? {
  return publicUserData?.identifier?.takeIf { it.isNotBlank() }
    ?: user.primaryEmailAddress?.emailAddress?.takeIf { it.isNotBlank() }
    ?: user.username?.takeIf { it.isNotBlank() }
}
