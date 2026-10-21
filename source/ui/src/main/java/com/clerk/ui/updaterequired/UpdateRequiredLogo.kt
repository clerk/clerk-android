package com.clerk.ui.updaterequired

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import com.clerk.ui.R
import com.clerk.ui.core.avatar.OrganizationLogo
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun InstanceOrAppLogo(organizationLogoUrl: String?) {
  if (organizationLogoUrl.isNullOrBlank()) {
    AppIconLogo()
  } else {
    OrganizationLogo(imageUrl = organizationLogoUrl)
  }
}

@Composable
internal fun AppIconLogo() {
  val context = LocalContext.current
  val size = ClerkMaterialTheme.design.logoMaxHeight
  val sizePx = with(LocalDensity.current) { size.roundToPx() }
  val icon = remember(context, sizePx) { appIcon(context, sizePx) } ?: return
  Image(
    bitmap = icon,
    contentDescription = stringResource(R.string.logo),
    modifier = Modifier.size(size),
  )
}

private fun appIcon(context: Context, sizePx: Int): ImageBitmap? {
  val icon: Drawable? = context.packageManager.getApplicationIcon(context.applicationInfo)
  return icon?.takeIf { sizePx > 0 }?.toBitmap(width = sizePx, height = sizePx)?.asImageBitmap()
}
