package com.clerk.ui.core.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp10
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun AvatarView(
  imageUrl: String?,
  size: AvatarSize,
  shape: Shape,
  avatarType: AvatarType,
  modifier: Modifier = Modifier,
) {
  val placeholder =
    when (avatarType) {
      AvatarType.USER -> R.drawable.ic_user
      AvatarType.ORGANIZATION -> R.drawable.ic_organization
    }

  Box(
    modifier =
      Modifier.wrapContentSize()
        .background(color = ClerkMaterialTheme.colors.primaryForeground, shape = shape)
        .padding(dp10),
    contentAlignment = Alignment.Center,
  ) {
    SubcomposeAsyncImage(
      model = imageUrl,
      contentDescription = stringResource(R.string.logo),
      modifier = Modifier.size(size.toDp()).clip(shape).then(modifier),
      contentScale = ContentScale.Fit,
      loading = { CircularProgressIndicator() },
      error = {
        Icon(
          painter = painterResource(placeholder),
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.foreground,
        )
      },
    )
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

@Composable
fun UserAvatar(modifier: Modifier = Modifier) {
  ClerkMaterialTheme {
    AvatarView(
      imageUrl = Clerk.user?.imageUrl,
      size = AvatarSize.MEDIUM,
      shape = CircleShape,
      modifier = modifier,
      avatarType = AvatarType.USER,
    )
  }
}

enum class AvatarSize {
  SMALL,
  MEDIUM,
  LARGE,
}

private fun AvatarSize.toDp(): Dp {
  return when (this) {
    AvatarSize.SMALL -> 24.dp
    AvatarSize.MEDIUM -> 36.dp
    AvatarSize.LARGE -> 48.dp
  }
}
