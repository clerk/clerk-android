package com.clerk.ui.core.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkButtonPadding
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp36
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.dimens.dp96
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun AvatarView(
  imageUrl: String?,
  size: AvatarSize,
  shape: Shape,
  avatarType: AvatarType,
  modifier: Modifier = Modifier,
  hasEditButton: Boolean = false,
  onEditTakePhoto: () -> Unit = {},
  onEditChoosePhoto: () -> Unit = {},
  onEditRemovePhoto: () -> Unit = {},
) {
  val placeholder =
    when (avatarType) {
      AvatarType.USER -> R.drawable.ic_user
      AvatarType.ORGANIZATION -> R.drawable.ic_organization
    }

  Box(modifier = Modifier.wrapContentSize().then(modifier), contentAlignment = Alignment.Center) {
    SubcomposeAsyncImage(
      model = imageUrl,
      contentDescription = stringResource(R.string.logo),
      modifier = Modifier.size(size.toDp()).clip(shape),
      contentScale = ContentScale.FillBounds,
      loading = { CircularProgressIndicator(modifier = Modifier.size(dp24)) },
      error = {
        Icon(
          painter = painterResource(placeholder),
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.foreground,
        )
      },
    )
    if (hasEditButton) {
      EditButton(
        onEditTakePhoto = onEditTakePhoto,
        onEditChoosePhoto = onEditChoosePhoto,
        onEditRemovePhoto = onEditRemovePhoto,
      )
    }
  }
}

@Composable
private fun BoxScope.EditButton(
  onEditTakePhoto: () -> Unit,
  onEditChoosePhoto: () -> Unit,
  onEditRemovePhoto: () -> Unit,
) {
  val expanded = remember { mutableStateOf(false) }
  Box(modifier = Modifier.align(Alignment.BottomEnd)) {
    ClerkButton(
      text = null,
      configuration =
        ClerkButtonConfiguration(
          size = ClerkButtonConfiguration.Size.Small,
          emphasis = ClerkButtonConfiguration.Emphasis.High,
          style = ClerkButtonConfiguration.ButtonStyle.Secondary,
          backgroundColorOverride = ClerkMaterialTheme.colors.muted,
        ),
      isEnabled = true,
      icons =
        ClerkButtonDefaults.icons(
          trailingIcon = R.drawable.ic_edit,
          trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
        ),
      padding = ClerkButtonPadding(horizontal = dp8, vertical = dp0),
      onClick = { expanded.value = true },
    )

    DropdownMenu(
      modifier =
        Modifier.background(ClerkMaterialTheme.colors.background).defaultMinSize(minWidth = 144.dp),
      expanded = expanded.value,
      onDismissRequest = { expanded.value = false },
    ) {
      DropdownMenuItem(
        text = { Text(text = stringResource(R.string.take_a_photo)) },
        onClick = {
          expanded.value = false
          onEditTakePhoto()
        },
      )
      DropdownMenuItem(
        text = { Text(text = stringResource(R.string.choose_photo)) },
        onClick = {
          expanded.value = false
          onEditChoosePhoto()
        },
      )
      DropdownMenuItem(
        text = {
          Text(
            text = stringResource(R.string.remove_photo),
            color = ClerkMaterialTheme.colors.danger,
          )
        },
        onClick = {
          expanded.value = false
          onEditRemovePhoto()
        },
      )
    }
  }
}

internal enum class AvatarType {
  USER,
  ORGANIZATION,
}

@Composable
fun OrganizationAvatar(
  modifier: Modifier = Modifier,
  shape: Shape? = null,
  size: AvatarSize = AvatarSize.MEDIUM,
) {
  val url = Clerk.organizationLogoUrl
  ClerkMaterialTheme {
    AvatarView(
      imageUrl = url,
      size = size,
      shape = shape ?: ClerkMaterialTheme.shape,
      modifier = modifier,
      avatarType = AvatarType.ORGANIZATION,
    )
  }
}

enum class AvatarSize {
  SMALL,
  MEDIUM,
  LARGE,
  X_LARGE,
}

private fun AvatarSize.toDp(): Dp {
  return when (this) {
    AvatarSize.SMALL -> dp24
    AvatarSize.MEDIUM -> dp36
    AvatarSize.LARGE -> dp48
    AvatarSize.X_LARGE -> dp96
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
      AvatarView(
        hasEditButton = true,
        imageUrl = null,
        size = AvatarSize.X_LARGE,
        shape = CircleShape,
        avatarType = AvatarType.USER,
      )
    }
  }
}
