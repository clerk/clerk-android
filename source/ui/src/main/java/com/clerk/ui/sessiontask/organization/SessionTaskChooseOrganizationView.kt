@file:Suppress("TooManyFunctions")

package com.clerk.ui.sessiontask.organization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.organizations.OrganizationCreationDefaults
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.organizations.UserOrganizationInvitation
import com.clerk.ui.R
import com.clerk.ui.auth.handleSessionTaskCompletion
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp14
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.footer.SecuredByClerkView
import com.clerk.ui.core.header.HeaderTextView
import com.clerk.ui.core.header.HeaderType
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun SessionTaskChooseOrganizationView(
  onAuthComplete: () -> Unit,
  onCreateOrganization: (OrganizationCreationDefaults?) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SessionTaskChooseOrganizationViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val authState = LocalAuthState.current

  LaunchedEffect(Unit) { viewModel.load() }
  LaunchedEffect(state.completedSession) {
    state.completedSession?.let {
      authState.handleSessionTaskCompletion(it, onAuthComplete)
      viewModel.clearCompletedSession()
    }
  }

  when {
    state.canShowNoOrganizationHelp -> SignInGetHelpView(modifier = modifier)
    else ->
      ChooseOrganizationContent(
        modifier = modifier,
        state = state,
        onErrorShown = viewModel::clearError,
        onRetryInitialLoad = viewModel::retryLoad,
        onCreateOrganization = { onCreateOrganization(state.creationDefaults) },
        onLoadMoreMemberships = viewModel::loadMoreMemberships,
        onLoadMoreInvitations = viewModel::loadMoreInvitations,
        onLoadMoreSuggestions = viewModel::loadMoreSuggestions,
        onSelectOrganization = viewModel::selectOrganization,
        onAcceptInvitation = viewModel::acceptInvitation,
        onAcceptSuggestion = viewModel::acceptSuggestion,
      )
  }
}

@Composable
@Suppress("LongMethod", "LongParameterList")
private fun ChooseOrganizationContent(
  state: SessionTaskChooseOrganizationState,
  onErrorShown: () -> Unit,
  onRetryInitialLoad: () -> Unit,
  onCreateOrganization: () -> Unit,
  onLoadMoreMemberships: () -> Unit,
  onLoadMoreInvitations: () -> Unit,
  onLoadMoreSuggestions: () -> Unit,
  onSelectOrganization: (String) -> Unit,
  onAcceptInvitation: (UserOrganizationInvitation) -> Unit,
  onAcceptSuggestion: (OrganizationSuggestion) -> Unit,
  modifier: Modifier = Modifier,
) {
  SessionTaskOrganizationScaffold(errorMessage = state.errorMessage, onErrorShown = onErrorShown) {
    innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding).then(modifier),
      contentAlignment = Alignment.Center,
    ) {
      if (state.isLoading) {
        CircularProgressIndicator()
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = dp18, vertical = dp16),
          verticalArrangement = Arrangement.spacedBy(dp12),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          item { ChooseOrganizationHeader(canCreateOrganization = state.canCreateOrganization) }

          if (state.initialLoadFailed) {
            item {
              LoadMoreButton(
                text = stringResource(R.string.try_again),
                isLoading = false,
                onClick = onRetryInitialLoad,
              )
            }
          }

          items(count = state.memberships.size, key = { state.memberships[it].id }) { index ->
            MembershipRow(
              membership = state.memberships[index],
              isLoading = state.activeActionId == state.memberships[index].organization.id,
              onClick = { onSelectOrganization(state.memberships[index].organization.id) },
            )
          }

          if (state.membershipsHasNextPage) {
            item {
              LoadMoreButton(
                isLoading = state.isLoadingMoreMemberships,
                onClick = onLoadMoreMemberships,
              )
            }
          }

          if (!state.membershipsHasNextPage) {
            items(count = state.invitations.size, key = { state.invitations[it].id }) { index ->
              val invitation = state.invitations[index]
              InvitationRow(
                invitation = invitation,
                isAccepted =
                  invitation.publicOrganizationData.id in state.acceptedInvitationOrganizationIds,
                isLoading = state.activeActionId == invitation.id,
                isSelecting = state.activeActionId == invitation.publicOrganizationData.id,
                onAccept = { onAcceptInvitation(invitation) },
                onSelect = { onSelectOrganization(invitation.publicOrganizationData.id) },
              )
            }

            if (state.invitationsHasNextPage) {
              item {
                LoadMoreButton(
                  isLoading = state.isLoadingMoreInvitations,
                  onClick = onLoadMoreInvitations,
                )
              }
            }
          }

          if (!state.membershipsHasNextPage && !state.invitationsHasNextPage) {
            items(count = state.suggestions.size, key = { state.suggestions[it].id }) { index ->
              val suggestion = state.suggestions[index]
              SuggestionRow(
                suggestion = suggestion,
                isLoading = state.activeActionId == suggestion.id,
                onAccept = { onAcceptSuggestion(suggestion) },
              )
            }

            if (state.suggestionsHasNextPage) {
              item {
                LoadMoreButton(
                  isLoading = state.isLoadingMoreSuggestions,
                  onClick = onLoadMoreSuggestions,
                )
              }
            }
          }

          if (
            state.hasLoadedInitialResources && !state.hasNextPage && state.canCreateOrganization
          ) {
            item { CreateOrganizationRow(onClick = onCreateOrganization) }
          }

          item {
            Spacer(modifier = Modifier.size(dp12))
            SecuredByClerkView()
          }
        }
      }
    }
  }
}

@Composable
private fun ChooseOrganizationHeader(canCreateOrganization: Boolean) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(bottom = dp20),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(dp8),
  ) {
    HeaderTextView(text = stringResource(R.string.choose_an_organization), type = HeaderType.Title)
    HeaderTextView(
      text =
        stringResource(
          if (canCreateOrganization) {
            R.string.join_an_existing_organization_or_create_a_new_one
          } else {
            R.string.join_an_existing_organization
          }
        ),
      type = HeaderType.Subtitle,
    )
  }
}

@Composable
private fun MembershipRow(
  membership: OrganizationMembership,
  isLoading: Boolean,
  onClick: () -> Unit,
) {
  OrganizationRow(
    name = membership.organization.name,
    imageUrl = membership.organization.imageUrl,
    subtitle = membership.roleName,
    isLoading = isLoading,
    onClick = onClick,
  )
}

@Composable
@Suppress("LongParameterList")
private fun InvitationRow(
  invitation: UserOrganizationInvitation,
  isAccepted: Boolean,
  isLoading: Boolean,
  isSelecting: Boolean,
  onAccept: () -> Unit,
  onSelect: () -> Unit,
) {
  val data = invitation.publicOrganizationData
  OrganizationRow(
    name = data.name,
    imageUrl = data.imageUrl,
    subtitle = if (isAccepted) displayRoleName(invitation.role) else null,
    isLoading = isSelecting,
    onClick = if (isAccepted) onSelect else null,
    action = {
      if (!isAccepted) {
        PillActionButton(
          text = stringResource(R.string.join),
          isLoading = isLoading,
          onClick = onAccept,
        )
      }
    },
  )
}

@Composable
private fun SuggestionRow(
  suggestion: OrganizationSuggestion,
  isLoading: Boolean,
  onAccept: () -> Unit,
) {
  OrganizationRow(
    name = suggestion.publicOrganizationData.name,
    imageUrl = suggestion.publicOrganizationData.imageUrl,
    subtitle =
      if (suggestion.status == ACCEPTED_STATUS) stringResource(R.string.pending_approval) else null,
    action = {
      if (suggestion.status != ACCEPTED_STATUS) {
        PillActionButton(
          text = stringResource(R.string.request_to_join),
          isLoading = isLoading,
          onClick = onAccept,
        )
      }
    },
  )
}

@Composable
private fun CreateOrganizationRow(onClick: () -> Unit) {
  OrganizationRow(
    name = stringResource(R.string.create_organization),
    imageUrl = null,
    leadingIcon = R.drawable.ic_plus,
    onClick = onClick,
  )
}

@Composable
private fun OrganizationRow(
  name: String,
  imageUrl: String?,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  leadingIcon: Int? = null,
  isLoading: Boolean = false,
  onClick: (() -> Unit)? = null,
  action: @Composable (() -> Unit)? = null,
) {
  val shape = ClerkMaterialTheme.shape
  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .then(modifier),
    shape = shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(dp16),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp12),
    ) {
      OrganizationAvatar(imageUrl = imageUrl, leadingIcon = leadingIcon, shape = shape)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dp4)) {
        Text(
          text = name,
          style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
        subtitle?.let {
          Text(
            text = it,
            style = ClerkMaterialTheme.typography.bodySmall,
            color = ClerkMaterialTheme.colors.mutedForeground,
          )
        }
      }
      when {
        isLoading -> CircularProgressIndicator(modifier = Modifier.size(dp24))
        action != null -> action()
        onClick != null ->
          Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = ClerkMaterialTheme.colors.mutedForeground,
          )
      }
    }
  }
}

@Composable
private fun OrganizationAvatar(imageUrl: String?, leadingIcon: Int?, shape: Shape) {
  Box(modifier = Modifier.size(dp48), contentAlignment = Alignment.Center) {
    if (leadingIcon == null) {
      AvatarView(
        imageUrl = imageUrl,
        size = AvatarSize.LARGE,
        shape = shape,
        avatarType = AvatarType.ORGANIZATION,
      )
    } else {
      Surface(
        modifier = Modifier.fillMaxSize(),
        shape = shape,
        color = ClerkMaterialTheme.colors.muted,
        border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            painter = painterResource(leadingIcon),
            contentDescription = null,
            tint = ClerkMaterialTheme.colors.mutedForeground,
          )
        }
      }
    }
  }
}

@Composable
internal fun PillActionButton(text: String, isLoading: Boolean = false, onClick: () -> Unit) {
  Surface(
    modifier = Modifier.clickable(enabled = !isLoading, onClick = onClick),
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.buttonBorder),
    shadowElevation = dp2,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = dp14, vertical = dp8),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp8),
    ) {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(dp14), strokeWidth = dp2)
      }
      Text(
        text = text,
        style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
        color = ClerkMaterialTheme.colors.foreground,
      )
    }
  }
}

@Composable
private fun LoadMoreButton(
  isLoading: Boolean,
  onClick: () -> Unit,
  text: String = stringResource(R.string.load_more),
) {
  ClerkButton(
    modifier = Modifier.fillMaxWidth(),
    text = text,
    isLoading = isLoading,
    onClick = onClick,
    configuration =
      ClerkButtonDefaults.configuration(
        style = ClerkButtonConfiguration.ButtonStyle.Secondary,
        emphasis = ClerkButtonConfiguration.Emphasis.High,
      ),
  )
}

private fun displayRoleName(role: String): String {
  return role.removePrefix("org:").replace("_", " ").replaceFirstChar { it.titlecase() }
}

private const val ACCEPTED_STATUS = "accepted"
