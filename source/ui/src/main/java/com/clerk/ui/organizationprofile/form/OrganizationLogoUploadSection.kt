package com.clerk.ui.organizationprofile.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp14
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.dimens.dp80
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun OrganizationAvatarLogoSection(
  logoModel: Any?,
  isLoading: Boolean,
  canRemoveLogo: Boolean,
  enabled: Boolean,
  actions: LogoUploadActions,
) {
  var menuExpanded by rememberSaveable { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(dp12),
  ) {
    LogoAvatarPreview(logoModel = logoModel, isLoading = isLoading)
    Box {
      LogoMenuButton(
        text =
          stringResource(if (logoModel == null) R.string.upload_logo else R.string.change_logo),
        isEnabled = enabled,
        onClick = { menuExpanded = true },
      )
      LogoUploadMenu(
        expanded = menuExpanded,
        canRemoveLogo = canRemoveLogo,
        onDismissRequest = { menuExpanded = false },
        actions =
          LogoUploadActions(
            onPhotoLibraryClick = {
              menuExpanded = false
              actions.onPhotoLibraryClick()
            },
            onChooseFileClick = {
              menuExpanded = false
              actions.onChooseFileClick()
            },
            onRemoveClick = {
              menuExpanded = false
              actions.onRemoveClick()
            },
          ),
      )
    }
  }
}

internal data class LogoUploadActions(
  val onPhotoLibraryClick: () -> Unit,
  val onChooseFileClick: () -> Unit,
  val onRemoveClick: () -> Unit,
)

@Composable
private fun LogoAvatarPreview(logoModel: Any?, isLoading: Boolean) {
  Box(modifier = Modifier.size(dp80), contentAlignment = Alignment.Center) {
    if (logoModel == null) {
      DashedLogoPlaceholder()
    } else {
      SubcomposeAsyncImage(
        model = logoModel,
        contentDescription = stringResource(R.string.logo),
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(dp8)),
        contentScale = ContentScale.Crop,
        loading = { CircularProgressIndicator(modifier = Modifier.size(dp24)) },
        error = { DashedLogoPlaceholder() },
      )
    }
    if (isLoading) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(dp12),
        color = ClerkMaterialTheme.colors.shadow.copy(alpha = 0.25f),
      ) {
        Box(contentAlignment = Alignment.Center) {
          CircularProgressIndicator(modifier = Modifier.size(dp24))
        }
      }
    }
  }
}

@Composable
private fun DashedLogoPlaceholder() {
  val borderColor = ClerkMaterialTheme.computedColors.inputBorder
  Surface(
    modifier =
      Modifier.fillMaxSize().drawBehind {
        drawRoundRect(
          color = borderColor,
          cornerRadius = CornerRadius(dp12.toPx(), dp12.toPx()),
          style =
            Stroke(
              width = dp1.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_WIDTH, DASH_GAP)),
            ),
        )
      },
    shape = RoundedCornerShape(dp12),
    color = ClerkMaterialTheme.colors.muted,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        modifier = Modifier.size(dp32),
        painter = painterResource(R.drawable.ic_organization),
        contentDescription = null,
        tint = ClerkMaterialTheme.colors.mutedForeground,
      )
    }
  }
}

@Composable
private fun LogoMenuButton(text: String, isEnabled: Boolean, onClick: () -> Unit) {
  Surface(
    modifier = Modifier.height(dp32).clickable(enabled = isEnabled, onClick = onClick),
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.buttonBorder),
    shadowElevation = dp2,
  ) {
    Box(modifier = Modifier.padding(horizontal = dp14), contentAlignment = Alignment.Center) {
      Text(
        text = text,
        style = ClerkMaterialTheme.typography.bodyMedium,
        color =
          if (isEnabled) {
            ClerkMaterialTheme.colors.foreground
          } else {
            ClerkMaterialTheme.colors.mutedForeground
          },
      )
    }
  }
}

@Composable
private fun LogoUploadMenu(
  expanded: Boolean,
  canRemoveLogo: Boolean,
  onDismissRequest: () -> Unit,
  actions: LogoUploadActions,
) {
  DropdownMenu(
    modifier = Modifier.width(LOGO_MENU_WIDTH),
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    shape = RoundedCornerShape(dp12),
    offset = DpOffset(x = 0.dp, y = dp8),
  ) {
    LogoUploadMenuItem(
      text = stringResource(R.string.photo_library),
      icon = R.drawable.ic_photo_library,
      onClick = actions.onPhotoLibraryClick,
    )
    LogoUploadMenuItem(
      text = stringResource(R.string.choose_file),
      icon = R.drawable.ic_folder,
      onClick = actions.onChooseFileClick,
    )
    if (canRemoveLogo) {
      DropdownMenuItem(
        text = { Text(text = stringResource(R.string.remove_current_logo)) },
        colors = MenuDefaults.itemColors(textColor = ClerkMaterialTheme.colors.danger),
        onClick = actions.onRemoveClick,
      )
    }
  }
}

@Composable
private fun LogoUploadMenuItem(text: String, icon: Int, onClick: () -> Unit) {
  DropdownMenuItem(
    leadingIcon = {
      Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = ClerkMaterialTheme.colors.foreground,
      )
    },
    text = { Text(text = text) },
    onClick = onClick,
  )
}

private val LOGO_MENU_WIDTH = 250.dp
private const val DASH_WIDTH = 8f
private const val DASH_GAP = 6f
