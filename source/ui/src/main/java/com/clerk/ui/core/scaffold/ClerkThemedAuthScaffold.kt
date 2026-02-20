package com.clerk.ui.core.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.clerk.api.Clerk
import com.clerk.api.user.User
import com.clerk.api.user.fullName
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp36
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.footer.SecuredByClerkView
import com.clerk.ui.core.header.HeaderTextView
import com.clerk.ui.core.header.HeaderType
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.coroutines.launch

@Composable
internal fun ClerkThemedAuthScaffold(
  title: String,
  modifier: Modifier = Modifier,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
  onBackPressed: () -> Unit = {},
  subtitle: String? = null,
  hasLogo: Boolean = true,
  hasBackButton: Boolean = true,
  identifier: String? = null,
  onClickIdentifier: () -> Unit = {},
  spacingAfterIdentifier: Dp = dp32,
  showSignedInUserButton: Boolean = true,
  content: @Composable () -> Unit,
) {
  val user = Clerk.userFlow.collectAsStateWithLifecycle().value
  val session = Clerk.sessionFlow.collectAsStateWithLifecycle().value
  var showSignedInAccountSheet by remember { mutableStateOf(false) }
  val displayName = user.displayName()
  val displayIdentifier = session?.publicUserData?.identifier ?: user?.username.orEmpty()
  val trailingContent =
    signedInTrailingContent(
      showSignedInUserButton = showSignedInUserButton,
      user = user,
      onClick = { showSignedInAccountSheet = true },
    )
  val scaffoldConfig =
    AuthScaffoldConfig(
      title = title,
      subtitle = subtitle,
      identifier = identifier,
      onClickIdentifier = onClickIdentifier,
      spacingAfterIdentifier = spacingAfterIdentifier,
    )

  ClerkMaterialTheme {
    Scaffold(
      modifier = Modifier.then(modifier),
      snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
      topBar = {
        ClerkTopAppBar(
          backgroundColor = ClerkMaterialTheme.colors.background,
          onBackPressed = onBackPressed,
          hasLogo = hasLogo,
          hasBackButton = hasBackButton,
          trailingContent = trailingContent,
        )
      },
    ) { innerPadding ->
      AuthScaffoldContent(innerPadding = innerPadding, config = scaffoldConfig, content = content)
    }

    AuthSignedInAccountSheetHost(
      user = user,
      showSignedInAccountSheet = showSignedInAccountSheet,
      displayName = displayName,
      displayIdentifier = displayIdentifier,
      onDismissRequest = { showSignedInAccountSheet = false },
    )
  }
}

@Composable
private fun AuthScaffoldContent(
  innerPadding: PaddingValues,
  config: AuthScaffoldConfig,
  content: @Composable () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(innerPadding)
        .padding(horizontal = dp18)
        .background(ClerkMaterialTheme.colors.background),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    HeaderTextView(text = config.title, type = HeaderType.Title)
    config.subtitle?.let {
      Spacers.Vertical.Spacer8()
      HeaderTextView(text = it, type = HeaderType.Subtitle)
    }
    config.identifier?.let {
      Spacers.Vertical.Spacer8()
      ClerkButton(
        paddingValues = PaddingValues(horizontal = dp8),
        modifier = Modifier.defaultMinSize(minWidth = 120.dp),
        text = it,
        onClick = config.onClickIdentifier,
        isEnabled = true,
        configuration =
          ClerkButtonDefaults.configuration(
            style = ClerkButtonConfiguration.ButtonStyle.Secondary,
            emphasis = ClerkButtonConfiguration.Emphasis.High,
          ),
        icons =
          ClerkButtonDefaults.icons(
            trailingIcon = R.drawable.ic_edit,
            trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
          ),
      )
    }
    Spacer(modifier = Modifier.height(config.spacingAfterIdentifier))
    content()
    Spacers.Vertical.Spacer32()
    SecuredByClerkView()
  }
}

private data class AuthScaffoldConfig(
  val title: String,
  val subtitle: String?,
  val identifier: String?,
  val onClickIdentifier: () -> Unit,
  val spacingAfterIdentifier: Dp,
)

private fun signedInTrailingContent(
  showSignedInUserButton: Boolean,
  user: User?,
  onClick: () -> Unit,
): (@Composable () -> Unit)? {
  if (!showSignedInUserButton || user == null) {
    return null
  }
  return { AuthSignedInAvatarButton(imageUrl = user.imageUrl, onClick = onClick) }
}

private fun User?.displayName(): String {
  return this?.fullName().orEmpty().takeIf { it.isNotBlank() } ?: this?.username.orEmpty()
}

@Composable
private fun AuthSignedInAvatarButton(
  imageUrl: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val avatarModel =
    remember(imageUrl, context) {
      ImageRequest.Builder(context).data(imageUrl).crossfade(true).build()
    }

  IconButton(modifier = modifier, onClick = onClick) {
    AsyncImage(
      modifier = Modifier.size(dp36).clip(CircleShape),
      model = avatarModel,
      contentDescription = stringResource(R.string.user_avatar),
      contentScale = ContentScale.Crop,
      fallback = painterResource(id = R.drawable.ic_profile),
      onError = { /* no-op */ },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthSignedInAccountSheet(
  imageUrl: String?,
  displayName: String,
  displayIdentifier: String,
  onDismissRequest: () -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  val avatarModel =
    remember(imageUrl, context) {
      ImageRequest.Builder(context).data(imageUrl).crossfade(true).build()
    }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = ClerkMaterialTheme.colors.background,
    contentColor = ClerkMaterialTheme.colors.foreground,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dp24, vertical = dp16)) {
      SignedInAccountSheetHeader(
        avatarModel = avatarModel,
        displayName = displayName,
        displayIdentifier = displayIdentifier,
      )
      Spacers.Vertical.Spacer12()
      HorizontalDivider(color = ClerkMaterialTheme.computedColors.border)
      SignOutActionRow {
        scope.launch {
          Clerk.auth.signOut()
          sheetState.hide()
          onDismissRequest()
        }
      }
    }
  }
}

@Composable
private fun AuthSignedInAccountSheetHost(
  user: User?,
  showSignedInAccountSheet: Boolean,
  displayName: String,
  displayIdentifier: String,
  onDismissRequest: () -> Unit,
) {
  if (showSignedInAccountSheet && user != null) {
    AuthSignedInAccountSheet(
      imageUrl = user.imageUrl,
      displayName = displayName,
      displayIdentifier = displayIdentifier,
      onDismissRequest = onDismissRequest,
    )
  }
}

@Composable
private fun SignedInAccountSheetHeader(
  avatarModel: ImageRequest,
  displayName: String,
  displayIdentifier: String,
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    AsyncImage(
      modifier = Modifier.size(dp36).clip(CircleShape),
      model = avatarModel,
      contentDescription = stringResource(R.string.user_avatar),
      contentScale = ContentScale.Crop,
      fallback = painterResource(id = R.drawable.ic_profile),
      onError = { /* no-op */ },
    )
    Column(modifier = Modifier.weight(1f).padding(start = dp12)) {
      if (displayName.isNotBlank()) {
        Text(
          text = displayName,
          style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
      }
      if (displayIdentifier.isNotBlank()) {
        Text(
          text = displayIdentifier,
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
    }
  }
}

@Composable
private fun SignOutActionRow(onClick: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = dp16),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(id = R.drawable.ic_sign),
      contentDescription = null,
      tint = ClerkMaterialTheme.colors.mutedForeground,
    )
    Text(
      modifier = Modifier.padding(start = dp12),
      text = stringResource(R.string.log_out),
      style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
      color = ClerkMaterialTheme.colors.foreground,
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewClerkThemedAuthScaffold() {
  ClerkThemedAuthScaffold(
    onBackPressed = {},
    title = "Welcome back",
    subtitle = "Sign in to continue",
    identifier = "sam@clerk.dev",
  ) {
    ClerkTextField(value = "", onValueChange = {})
  }
}
