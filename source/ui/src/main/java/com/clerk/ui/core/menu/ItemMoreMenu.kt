package com.clerk.ui.core.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> ItemMoreMenu(
  dropDownItems: ImmutableList<DropDownItem<T>>,
  onClick: (T) -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector = Icons.Outlined.MoreVert,
  iconTint: Color = ClerkMaterialTheme.colors.mutedForeground,
  menuContentDescription: String = stringResource(R.string.more_options),
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  val onClickUpdated by rememberUpdatedState(onClick)

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    IconButton(
      modifier =
        modifier
          .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
          .semantics { role = Role.Button },
      onClick = { expanded = true },
    ) {
      Icon(imageVector = icon, contentDescription = menuContentDescription, tint = iconTint)
    }

    DropdownMenu(
      modifier = Modifier.width(IntrinsicSize.Min),
      expanded = expanded,
      onDismissRequest = { expanded = false },
      shape = ClerkMaterialTheme.shape,
    ) {
      dropDownItems.forEach { item ->
        if (item.isHidden) return@forEach
        DropdownMenuItem(
          leadingIcon =
            item.leadingIcon?.let { iconVec ->
              { Icon(imageVector = iconVec, contentDescription = null) }
            },
          text = { Text(text = item.text, style = ClerkMaterialTheme.typography.bodyLarge) },
          enabled = item.enabled,
          colors =
            MenuDefaults.itemColors(
              textColor =
                if (item.danger) ClerkMaterialTheme.colors.danger else LocalContentColor.current
            ),
          onClick = {
            expanded = false
            onClickUpdated(item.id)
          },
        )
      }
    }
  }
}

internal data class DropDownItem<T>(
  val id: T,
  val text: String,
  val leadingIcon: ImageVector? = null,
  val enabled: Boolean = true,
  val danger: Boolean = false,
  val isHidden: Boolean = false,
)

enum class PreviewItemMoreMenu {
  VERIFY,
  SET_AS_PRIMARY,
  REMOVE_EMAIL,
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Row(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
      Spacer(modifier = Modifier.weight(1f))
      ItemMoreMenu(
        dropDownItems =
          persistentListOf<DropDownItem<PreviewItemMoreMenu>>(
            DropDownItem(
              id = PreviewItemMoreMenu.VERIFY,
              text = stringResource(R.string.verify),
              leadingIcon = Icons.Outlined.MoreVert, // example
            ),
            DropDownItem(
              id = PreviewItemMoreMenu.REMOVE_EMAIL,
              text = stringResource(R.string.set_as_primary),
            ),
            DropDownItem(
              id = PreviewItemMoreMenu.SET_AS_PRIMARY,
              text = stringResource(R.string.remove_email),
              danger = true,
            ),
          ),
        onClick = {},
      )
    }
  }
}
