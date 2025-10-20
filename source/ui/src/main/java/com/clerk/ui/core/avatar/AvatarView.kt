package com.clerk.ui.core.avatar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
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
import coil3.compose.SubcomposeAsyncImage
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
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
