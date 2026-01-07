package com.clerk.ui.core.avatar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp3
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp36
import com.clerk.ui.core.dimens.dp48
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
  var expanded by remember { mutableStateOf(false) }
  Box(modifier = Modifier.align(Alignment.BottomEnd)) {
    Surface(
      modifier =
        Modifier.size(dp32)
          .shadow(
            elevation = dp3,
            spotColor = ClerkMaterialTheme.colors.shadow.copy(alpha = 0.08f),
            ambientColor = ClerkMaterialTheme.colors.shadow.copy(alpha = 0.08f),
            shape = ClerkMaterialTheme.shape,
          ),
      border = BorderStroke(dp1, color = ClerkMaterialTheme.colors.shadow.copy(alpha = 0.08f)),
      shape = ClerkMaterialTheme.shape,
      onClick = { expanded = true },
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          painter = painterResource(R.drawable.ic_edit),
          contentDescription = stringResource(R.string.edit_avatar),
          tint = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
    }

    DropdownMenu(
      expanded,
      onEditTakePhoto,
      onEditChoosePhoto,
      onEditRemovePhoto,
      onDismissRequest = { expanded = false },
    )
  }
}

@Composable
private fun DropdownMenu(
  expanded: Boolean,
  onEditTakePhoto: () -> Unit,
  onEditChoosePhoto: () -> Unit,
  onEditRemovePhoto: () -> Unit,
  onDismissRequest: () -> Unit,
) {

  DropdownMenu(
    modifier =
      Modifier.background(ClerkMaterialTheme.colors.background).defaultMinSize(minWidth = 144.dp),
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    offset = DpOffset(0.dp, dp12),
    border =
      BorderStroke(
        width = dp1,
        color = ClerkMaterialTheme.computedColors.border.copy(alpha = 0.06f),
      ),
  ) {
    DropdownMenuItem(
      text = {
        Text(
          text = stringResource(R.string.take_a_photo),
          style = ClerkMaterialTheme.typography.bodyLarge,
        )
      },
      onClick = {
        onDismissRequest()
        onEditTakePhoto()
      },
    )
    DropdownMenuItem(
      text = {
        Text(
          text = stringResource(R.string.choose_photo),
          style = ClerkMaterialTheme.typography.bodyLarge,
        )
      },
      onClick = {
        onDismissRequest()
        onEditChoosePhoto()
      },
    )
    DropdownMenuItem(
      text = {
        Text(
          text = stringResource(R.string.remove_photo),
          color = ClerkMaterialTheme.colors.danger,
          style = ClerkMaterialTheme.typography.bodyLarge,
        )
      },
      onClick = {
        onDismissRequest()
        onEditRemovePhoto()
      },
    )
  }
}

internal enum class AvatarType {
  USER,
  ORGANIZATION,
}

@Composable
internal fun OrganizationAvatar(
  modifier: Modifier = Modifier,
  shape: Shape? = null,
  size: AvatarSize = AvatarSize.MEDIUM,
  clerkTheme: ClerkTheme? = null,
) {
  val url = Clerk.organizationLogoUrl
  ClerkMaterialTheme(clerkTheme = clerkTheme) {
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
    Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background).padding(dp12)) {
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
