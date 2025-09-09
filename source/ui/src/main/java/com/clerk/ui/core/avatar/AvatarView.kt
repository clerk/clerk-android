package com.clerk.ui.core.avatar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun AvatarView(
  imageUrl: String?,
  size: AvatarSize,
  shape: Shape,
  modifier: Modifier = Modifier,
) {
  Box(contentAlignment = Alignment.Center, modifier = modifier) {
    SubcomposeAsyncImage(
      model = imageUrl,
      contentDescription = null,
      modifier = Modifier.size(size.toDp()).clip(shape).then(modifier),
      contentScale = ContentScale.Fit,
      loading = { CircularProgressIndicator() },
      error = {
        Icon(
          painter = painterResource(id = R.drawable.ic_profile),
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.foreground,
        )
      },
    )
  }
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
