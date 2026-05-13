@file:Suppress("LongParameterList", "TooManyFunctions")

package com.clerk.ui.organizationswitcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.clerk.api.Clerk
import com.clerk.api.organizations.OrganizationCreationDefaults
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.user.User
import com.clerk.api.user.fullName
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.core.composition.TelemetryProvider
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp36
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.organizationlist.OrganizationAccountListActions
import com.clerk.ui.organizationlist.OrganizationAccountListState
import com.clerk.ui.organizationlist.OrganizationAccountListViewModel
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import com.clerk.ui.userbutton.UserButton

/**
 * Controls how [OrganizationSwitcher] renders its trigger.
 *
 * [Normal] shows the active account avatar, account name, and chevron. [Compact] shows only the
 * avatar-sized account trigger. Use [normal] or [compact] when a custom trigger avatar size is
 * needed.
 */
sealed class OrganizationSwitcherDisplayMode
private constructor(internal val isCompact: Boolean, val size: Dp) {
  data object Normal : OrganizationSwitcherDisplayMode(isCompact = false, size = dp36)

  data object Compact : OrganizationSwitcherDisplayMode(isCompact = true, size = dp36)

  private class Custom(isCompact: Boolean, size: Dp) :
    OrganizationSwitcherDisplayMode(isCompact = isCompact, size = size)

  companion object {
    fun normal(size: Dp): OrganizationSwitcherDisplayMode = Custom(isCompact = false, size = size)

    fun compact(size: Dp): OrganizationSwitcherDisplayMode = Custom(isCompact = true, size = size)
  }
}

/**
 * Self-contained active organization switcher.
 *
 * Drop this into a top app bar or page header. When organizations are enabled for the instance, it
 * renders the active organization or personal account and opens account management sheets for
 * switching between personal and organization accounts.
 *
 * @param clerkTheme Optional theme customization for the switcher UI.
 * @param showUserButton When `true` (default), render [UserButton] on the trailing edge to match
 *   Clerk's mobile prebuilt header pattern.
 * @param onOrganizationChanged Optional callback invoked after a successful organization or
 *   personal-account switch.
 * @param hidePersonal Hides personal account selection when the instance does not force
 *   organization selection.
 * @param displayMode Controls whether the trigger renders in normal or compact form.
 * @param onManageOrganization Called when the active organization overview's manage action is
 *   selected. The action is hidden when this callback is `null`.
 * @param onCreateOrganization Called when the create-organization row is selected from the switch
 *   account sheet. The row is hidden when this callback is `null`.
 */
@Composable
fun OrganizationSwitcher(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  showUserButton: Boolean = true,
  onOrganizationChanged: (() -> Unit)? = null,
  hidePersonal: Boolean = false,
  displayMode: OrganizationSwitcherDisplayMode = OrganizationSwitcherDisplayMode.Normal,
  onManageOrganization: ((OrganizationMembership) -> Unit)? = null,
  onCreateOrganization: ((OrganizationCreationDefaults?) -> Unit)? = null,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    ClerkMaterialTheme {
      TelemetryProvider {
        OrganizationSwitcherImpl(
          modifier = modifier,
          clerkTheme = clerkTheme,
          showUserButton = showUserButton,
          onOrganizationChanged = onOrganizationChanged,
          hidePersonal = hidePersonal,
          displayMode = displayMode,
          onManageOrganization = onManageOrganization,
          onCreateOrganization = onCreateOrganization,
        )
      }
    }
  }
}

@Composable
internal fun OrganizationSwitcherImpl(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  showUserButton: Boolean = true,
  onOrganizationChanged: (() -> Unit)? = null,
  hidePersonal: Boolean = false,
  displayMode: OrganizationSwitcherDisplayMode = OrganizationSwitcherDisplayMode.Normal,
  onManageOrganization: ((OrganizationMembership) -> Unit)? = null,
  onCreateOrganization: ((OrganizationCreationDefaults?) -> Unit)? = null,
  viewModel: OrganizationAccountListViewModel = viewModel(),
) {
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  val state by viewModel.state.collectAsStateWithLifecycle()
  var sheetDestination by rememberSaveable {
    mutableStateOf<OrganizationSwitcherSheetDestination?>(null)
  }

  val activeMembership = activeOrganizationMembership(user, session, state.memberships)
  val showPersonalAccount = user != null && !hidePersonal && !Clerk.organizationSelectionIsForced

  OrganizationSwitcherEffects(
    userId = user?.id,
    sessionId = session?.id,
    hasUser = user != null,
    viewModel = viewModel,
  )

  val shouldShow =
    shouldShowOrganizationSwitcher(
      hasUser = user != null,
      hasSession = session != null,
      organizationsEnabled = Clerk.organizationIsEnabled,
    )

  if (shouldShow) {
    OrganizationSwitcherHeader(
      modifier = modifier,
      activeMembership = activeMembership,
      user = user,
      showPersonalAccount = showPersonalAccount,
      isLoading = state.isLoading && activeMembership == null && !showPersonalAccount,
      showUserButton = showUserButton,
      clerkTheme = clerkTheme,
      displayMode = displayMode,
      onClick = {
        sheetDestination =
          if (activeMembership == null) {
            OrganizationSwitcherSheetDestination.AccountList
          } else {
            OrganizationSwitcherSheetDestination.Overview
          }
      },
    )
  }

  OrganizationSwitcherActiveSheet(
    destination = sheetDestination,
    state = state,
    user = user,
    activeMembership = activeMembership,
    activeOrganizationId = session?.lastActiveOrganizationId,
    showPersonalAccount = showPersonalAccount,
    onDismiss = { sheetDestination = null },
    onShowAccountList = { sheetDestination = OrganizationSwitcherSheetDestination.AccountList },
    onManageOrganization = onManageOrganization,
    onOrganizationChanged = onOrganizationChanged,
    onCreateOrganization = onCreateOrganization,
    viewModel = viewModel,
  )
}

@Composable
private fun OrganizationSwitcherEffects(
  userId: String?,
  sessionId: String?,
  hasUser: Boolean,
  viewModel: OrganizationAccountListViewModel,
) {
  val telemetry = LocalTelemetryCollector.current
  LaunchedEffect(userId, sessionId) {
    viewModel.reset()
    viewModel.load()
  }
  LaunchedEffect(userId) {
    if (hasUser) telemetry.record(TelemetryEvents.viewDidAppear("OrganizationSwitcher"))
  }
}

@Composable
private fun OrganizationSwitcherActiveSheet(
  destination: OrganizationSwitcherSheetDestination?,
  state: OrganizationAccountListState,
  user: User?,
  activeMembership: OrganizationMembership?,
  activeOrganizationId: String?,
  showPersonalAccount: Boolean,
  onDismiss: () -> Unit,
  onShowAccountList: () -> Unit,
  onManageOrganization: ((OrganizationMembership) -> Unit)?,
  onOrganizationChanged: (() -> Unit)?,
  onCreateOrganization: ((OrganizationCreationDefaults?) -> Unit)?,
  viewModel: OrganizationAccountListViewModel,
) {
  destination ?: return

  OrganizationSwitcherSheet(
    destination = destination,
    state = state,
    user = user,
    activeMembership = activeMembership,
    activeOrganizationId = activeOrganizationId,
    showPersonalAccount = showPersonalAccount,
    showCreateOrganization = onCreateOrganization != null,
    onDismiss = onDismiss,
    onShowAccountList = onShowAccountList,
    onManageOrganization =
      onManageOrganization?.let { manageOrganization ->
        { membership: OrganizationMembership ->
          onDismiss()
          manageOrganization(membership)
        }
      },
    onErrorShown = viewModel::clearError,
    actions =
      organizationSwitcherAccountListActions(
        state = state,
        viewModel = viewModel,
        onDismiss = onDismiss,
        onOrganizationChanged = onOrganizationChanged,
        onCreateOrganization = onCreateOrganization,
      ),
  )
}

@Composable
private fun OrganizationSwitcherHeader(
  activeMembership: OrganizationMembership?,
  user: User?,
  showPersonalAccount: Boolean,
  isLoading: Boolean,
  showUserButton: Boolean,
  clerkTheme: ClerkTheme?,
  displayMode: OrganizationSwitcherDisplayMode,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(dp12),
  ) {
    OrganizationSwitcherButton(
      modifier = if (displayMode.isCompact) Modifier else Modifier.weight(1f),
      membership = activeMembership,
      user = user,
      showPersonalAccount = showPersonalAccount,
      displayMode = displayMode,
      isLoading = isLoading,
      onClick = onClick,
    )
    if (displayMode.isCompact) {
      Spacer(modifier = Modifier.weight(1f))
    }
    if (showUserButton) {
      UserButton(clerkTheme = clerkTheme)
    }
  }
}

@Composable
internal fun OrganizationSwitcherButton(
  membership: OrganizationMembership?,
  isLoading: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  user: User? = null,
  showPersonalAccount: Boolean = true,
  displayMode: OrganizationSwitcherDisplayMode = OrganizationSwitcherDisplayMode.Normal,
) {
  val content =
    remember(membership, user, showPersonalAccount) {
      organizationSwitcherTriggerContent(
        membership = membership,
        user = user,
        showPersonalAccount = showPersonalAccount,
      )
    }
  val openOrganizationSwitcherDescription = stringResource(R.string.open_organization_switcher)
  val clickableModifier =
    modifier.clickable(enabled = !isLoading, onClick = onClick).semantics {
      contentDescription = openOrganizationSwitcherDescription
    }

  if (displayMode.isCompact) {
    CompactOrganizationSwitcherButton(
      modifier = clickableModifier,
      content = content,
      displayMode = displayMode,
      isLoading = isLoading,
    )
  } else {
    NormalOrganizationSwitcherButton(
      modifier = clickableModifier,
      content = content,
      displayMode = displayMode,
      isLoading = isLoading,
    )
  }
}

@Composable
private fun NormalOrganizationSwitcherButton(
  content: OrganizationSwitcherTriggerContent,
  displayMode: OrganizationSwitcherDisplayMode,
  isLoading: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(dp12),
  ) {
    OrganizationSwitcherAccountAvatar(
      imageUrl = content.imageUrl,
      avatarType = content.avatarType,
      shape = content.avatarShape(),
      size = displayMode.size,
    )
    Text(
      modifier = Modifier.weight(1f, fill = false),
      text = content.label ?: stringResource(R.string.select_organization),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
      color = ClerkMaterialTheme.colors.foreground,
    )
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(dp24), strokeWidth = dp2)
    } else {
      Icon(
        modifier = Modifier.size(dp24),
        painter = painterResource(R.drawable.ic_chevron_down),
        contentDescription = null,
        tint = ClerkMaterialTheme.colors.mutedForeground,
      )
    }
  }
}

@Composable
private fun CompactOrganizationSwitcherButton(
  content: OrganizationSwitcherTriggerContent,
  displayMode: OrganizationSwitcherDisplayMode,
  isLoading: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.size(displayMode.size.coerceAtLeast(dp48)),
    contentAlignment = Alignment.Center,
  ) {
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(dp24), strokeWidth = dp2)
    } else {
      OrganizationSwitcherAccountAvatar(
        imageUrl = content.imageUrl,
        avatarType = content.avatarType,
        shape = content.avatarShape(),
        size = displayMode.size,
      )
    }
  }
}

@Composable
private fun OrganizationSwitcherAccountAvatar(
  imageUrl: String?,
  avatarType: AvatarType,
  shape: Shape,
  size: Dp,
) {
  Surface(
    modifier = Modifier.size(size),
    shape = shape,
    color = ClerkMaterialTheme.colors.muted,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      if (imageUrl.isNullOrBlank()) {
        OrganizationSwitcherAvatarPlaceholder(size = size, avatarType = avatarType)
      } else {
        SubcomposeAsyncImage(
          model = imageUrl,
          contentDescription = stringResource(R.string.logo),
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
          loading = { OrganizationSwitcherAvatarPlaceholder(size = size, avatarType = avatarType) },
          error = { OrganizationSwitcherAvatarPlaceholder(size = size, avatarType = avatarType) },
        )
      }
    }
  }
}

@Composable
private fun OrganizationSwitcherAvatarPlaceholder(size: Dp, avatarType: AvatarType) {
  Icon(
    modifier = Modifier.size(size * AVATAR_PLACEHOLDER_SIZE_RATIO),
    painter =
      painterResource(
        when (avatarType) {
          AvatarType.USER -> R.drawable.ic_user
          AvatarType.ORGANIZATION -> R.drawable.ic_organization
        }
      ),
    contentDescription = null,
    tint = ClerkMaterialTheme.colors.foreground,
  )
}

private fun organizationSwitcherAccountListActions(
  state: OrganizationAccountListState,
  viewModel: OrganizationAccountListViewModel,
  onDismiss: () -> Unit,
  onOrganizationChanged: (() -> Unit)?,
  onCreateOrganization: ((OrganizationCreationDefaults?) -> Unit)?,
): OrganizationAccountListActions {
  return OrganizationAccountListActions(
    onRetryInitialLoad = viewModel::retryLoad,
    onLoadMoreMemberships = viewModel::loadMoreMemberships,
    onLoadMoreInvitations = viewModel::loadMoreInvitations,
    onLoadMoreSuggestions = viewModel::loadMoreSuggestions,
    onSelectPersonalAccount = {
      viewModel.selectPersonalAccount {
        onDismiss()
        onOrganizationChanged?.invoke()
      }
    },
    onSelectOrganization = { organizationId ->
      viewModel.selectOrganization(organizationId) {
        onDismiss()
        onOrganizationChanged?.invoke()
      }
    },
    onAcceptInvitation = viewModel::acceptInvitation,
    onAcceptSuggestion = viewModel::acceptSuggestion,
    onCreateOrganization = { onCreateOrganization?.invoke(state.creationDefaults) },
  )
}

private data class OrganizationSwitcherTriggerContent(
  val label: String?,
  val imageUrl: String?,
  val avatarType: AvatarType,
)

@Composable
private fun OrganizationSwitcherTriggerContent.avatarShape(): Shape {
  return if (avatarType == AvatarType.USER) CircleShape else ClerkMaterialTheme.shape
}

private fun organizationSwitcherTriggerContent(
  membership: OrganizationMembership?,
  user: User?,
  showPersonalAccount: Boolean,
): OrganizationSwitcherTriggerContent {
  return when {
    membership != null ->
      OrganizationSwitcherTriggerContent(
        label = membership.organization.name,
        imageUrl = membership.organization.imageUrl,
        avatarType = AvatarType.ORGANIZATION,
      )
    showPersonalAccount && user != null ->
      OrganizationSwitcherTriggerContent(
        label = user.displayName().ifBlank { null },
        imageUrl = user.imageUrl,
        avatarType = AvatarType.USER,
      )
    else ->
      OrganizationSwitcherTriggerContent(
        label = null,
        imageUrl = null,
        avatarType = AvatarType.ORGANIZATION,
      )
  }
}

private fun User.displayName(): String {
  return fullName()
    .ifBlank { username.orEmpty() }
    .ifBlank { primaryEmailAddress?.emailAddress.orEmpty() }
}

internal enum class OrganizationSwitcherSheetDestination {
  Overview,
  AccountList,
}

private const val AVATAR_PLACEHOLDER_SIZE_RATIO = 0.55f
