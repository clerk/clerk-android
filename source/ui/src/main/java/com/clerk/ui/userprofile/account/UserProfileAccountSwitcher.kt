package com.clerk.ui.userprofile.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.clerk.api.*
import com.clerk.api.Session
import com.clerk.api.User
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import com.clerk.ui.core.composition.LocalClerk
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
internal fun UserProfileAccountSwitcherSheet(
  onDismissRequest: () -> Unit,
  onAddAccount: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val clerk = LocalClerk.current
  ClerkMaterialTheme {
    val sessions = clerk.sessions
    val currentSession = clerk.session
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var loadingActionId by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val sortedSessions =
      sessions.sortedWith(
        compareByDescending<Session> { it.id == currentSession?.id }
          .thenByDescending { it.lastActiveAt }
      )

    ModalBottomSheet(
      modifier = modifier,
      onDismissRequest = onDismissRequest,
      sheetState = sheetState,
      containerColor = ClerkMaterialTheme.colors.background,
      contentColor = ClerkMaterialTheme.colors.foreground,
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          modifier = Modifier.padding(horizontal = dp24, vertical = dp16),
          text = stringResource(R.string.switch_account),
          style = ClerkMaterialTheme.typography.titleMedium.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
        HorizontalDivider(color = ClerkMaterialTheme.computedColors.border)

        sortedSessions.forEach { session ->
          val user = session.user
          if (user != null) {
            AccountSessionRow(
              session = session,
              user = user,
              isCurrent = session.id == currentSession?.id,
              isLoading = loadingActionId == session.id,
              onClick = {
                if (session.id == currentSession?.id || loadingActionId != null)
                  return@AccountSessionRow
                loadingActionId = session.id
                errorMessage = null
                scope.launch {
                  runUiOperation {
                      clerk.setActive(
                        MobileSetActiveParams(
                          session = Field.Value(MobileSetActiveParamsSession.Case1(session.id)),
                          organization =
                            session.lastActiveOrganizationId?.let {
                              Field.Value(MobileSetActiveParamsOrganization.Case1(it))
                            } ?: Field.Null,
                        )
                      )
                    }
                    .onSuccess {
                      loadingActionId = null
                      sheetState.hide()
                      onDismissRequest()
                    }
                    .onFailure {
                      loadingActionId = null
                      errorMessage = it.displayMessage
                    }
                }
              },
            )
            HorizontalDivider(color = ClerkMaterialTheme.computedColors.border)
          }
        }

        AccountActionRow(
          iconResId = R.drawable.ic_plus,
          text = stringResource(R.string.add_account),
          enabled = loadingActionId == null,
          onClick = {
            onDismissRequest()
            onAddAccount()
          },
        )
        HorizontalDivider(color = ClerkMaterialTheme.computedColors.border)
        AccountActionRow(
          iconResId = R.drawable.ic_sign,
          text = stringResource(R.string.sign_out_of_all_accounts),
          enabled = loadingActionId == null,
          isLoading = loadingActionId == SIGN_OUT_ALL_ACTION_ID,
          onClick = {
            if (loadingActionId != null) return@AccountActionRow
            loadingActionId = SIGN_OUT_ALL_ACTION_ID
            errorMessage = null
            scope.launch {
              runUiOperation { clerk.signOut() }
                .onSuccess {
                  loadingActionId = null
                  sheetState.hide()
                  onDismissRequest()
                }
                .onFailure {
                  loadingActionId = null
                  errorMessage = it.displayMessage
                }
            }
          },
        )

        errorMessage?.let {
          Text(
            modifier = Modifier.padding(horizontal = dp24, vertical = dp12),
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
private fun AccountSessionRow(
  session: Session,
  user: User,
  isCurrent: Boolean,
  isLoading: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(enabled = !isCurrent && !isLoading, onClick = onClick)
        .padding(horizontal = dp24, vertical = dp16),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AvatarView(
      size = AvatarSize.MEDIUM,
      shape = CircleShape,
      avatarType = AvatarType.USER,
      imageUrl = user.imageUrl,
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
      session.displayIdentifier(user)?.let {
        Text(
          text = it,
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
    }
    when {
      isLoading -> CircularProgressIndicator(modifier = Modifier.size(dp20), strokeWidth = dp2)
      isCurrent ->
        Icon(
          modifier = Modifier.size(dp20),
          painter = painterResource(R.drawable.ic_check),
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.primary,
        )
    }
  }
}

@Composable
private fun AccountActionRow(
  iconResId: Int,
  text: String,
  enabled: Boolean,
  onClick: () -> Unit,
  isLoading: Boolean = false,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = dp24, vertical = dp16),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      modifier = Modifier.size(dp20),
      painter = painterResource(iconResId),
      contentDescription = null,
      tint = ClerkMaterialTheme.colors.mutedForeground,
    )
    Text(
      modifier = Modifier.weight(1f).padding(start = dp12),
      text = text,
      style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
      color = ClerkMaterialTheme.colors.foreground,
    )
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(dp20), strokeWidth = dp2)
    }
  }
}

private fun User.displayName(): String = fullName.orEmpty().ifBlank { username.orEmpty() }

private fun Session.displayIdentifier(user: User): String? {
  return publicUserData?.identifier?.takeIf { it.isNotBlank() }
    ?: user.username?.takeIf { it.isNotBlank() }
    ?: user.emailAddresses
      ?.firstOrNull { it.id == user.primaryEmailAddressId }
      ?.emailAddress
      ?.takeIf { it.isNotBlank() }
}

private const val SIGN_OUT_ALL_ACTION_ID = "sign_out_all"
