package com.clerk.ui.core.input

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun ClerkTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  @DrawableRes leadingIcon: Int? = null,
  @DrawableRes trailingIcon: Int? = null,
  label: String? = null,
  placeholder: String? = null,
  supportingText: String? = null,
  isError: Boolean = false,
  enabled: Boolean = true,
  onLeadingIconClick: () -> Unit = {},
  onTrailingIconClick: () -> Unit = {},
  inputContentType: ContentType = ContentType.Username,
  leadingIconContentDescription: String? = null,
  trailingIconContentDescription: String? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  val textFieldColors =
    OutlinedTextFieldDefaults.colors(
      focusedBorderColor = ClerkMaterialTheme.colors.primary,
      focusedLabelColor = ClerkMaterialTheme.colors.primary,
      unfocusedBorderColor = ClerkMaterialTheme.computedColors.inputBorder,
      unfocusedTextColor = ClerkMaterialTheme.colors.foreground,
      unfocusedContainerColor = ClerkMaterialTheme.colors.background,
      focusedContainerColor = ClerkMaterialTheme.colors.background,
      errorBorderColor = ClerkMaterialTheme.colors.danger,
      errorSupportingTextColor = ClerkMaterialTheme.colors.danger,
    )

  val labelStyle =
    if (isFocused || value.isNotEmpty()) ClerkMaterialTheme.typography.bodySmall
    else MaterialTheme.typography.bodyLarge
  val labelColor =
    when {
      isError -> ClerkMaterialTheme.colors.danger
      isFocused -> ClerkMaterialTheme.colors.primary
      else -> ClerkMaterialTheme.colors.mutedForeground
    }

  ClerkMaterialTheme {
    OutlinedTextField(
      interactionSource = interactionSource,
      modifier = modifier.fillMaxWidth().semantics { contentType = inputContentType },
      value = value,
      onValueChange = onValueChange,
      enabled = enabled,
      shape = ClerkMaterialTheme.shape,
      isError = isError,
      colors = textFieldColors,
      leadingIcon =
        leadingIcon?.let { resId ->
          {
            ClickableIcon(
              resId = resId,
              onClick = onLeadingIconClick,
              contentDescription = leadingIconContentDescription,
            )
          }
        },
      trailingIcon = {
        if (trailingIcon != null || isError) {
          val resId = if (isError) R.drawable.ic_warning else trailingIcon!!
          val tint =
            if (isError) ClerkMaterialTheme.colors.danger
            else ClerkMaterialTheme.colors.mutedForeground
          ClickableIcon(
            resId = resId,
            onClick = onTrailingIconClick,
            tint = tint,
            contentDescription = trailingIconContentDescription,
          )
        }
      },
      placeholder = placeholder?.let { ph -> { Text(ph) } },
      label = label?.let { text -> { Text(text = text, style = labelStyle, color = labelColor) } },
      textStyle = ClerkMaterialTheme.typography.bodyLarge,
      supportingText =
        supportingText?.let { support ->
          {
            Text(
              modifier = Modifier.padding(top = dp4),
              text = support,
              style = ClerkMaterialTheme.typography.bodySmall,
              color =
                if (isError) ClerkMaterialTheme.colors.danger
                else ClerkMaterialTheme.colors.mutedForeground,
            )
          }
        },
    )
  }
}

@Composable
private fun ClickableIcon(
  @DrawableRes resId: Int,
  onClick: () -> Unit,
  tint: androidx.compose.ui.graphics.Color = ClerkMaterialTheme.colors.mutedForeground,
  contentDescription: String? = null,
) {
  Icon(
    modifier = Modifier.size(dp24).clickable { onClick() },
    painter = painterResource(resId),
    contentDescription = contentDescription,
    tint = tint,
  )
}

@PreviewLightDark
@Composable
private fun PreviewClerkTextField() {
  ClerkMaterialTheme {
    LazyColumn(
      modifier = Modifier.background(color = ClerkMaterialTheme.colors.background).padding(dp12),
      verticalArrangement = Arrangement.spacedBy(dp20, alignment = Alignment.CenterVertically),
    ) {
      item {
        ClerkTextField(
          value = "Input",
          onValueChange = {},
          label = "Label",
          trailingIcon = R.drawable.ic_cross,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          value = "",
          onValueChange = {},
          label = "Label",
          trailingIcon = R.drawable.ic_cross,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          value = "Input",
          onValueChange = {},
          placeholder = "Placeholder",
          label = "Label",
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          value = "",
          onValueChange = {},
          label = "Label",
          supportingText = "Supporting text",
          leadingIcon = R.drawable.ic_search,
        )
      }
      item {
        ClerkTextField(
          value = "Input",
          onValueChange = {},
          label = "Label",
          trailingIcon = R.drawable.ic_cross,
          leadingIcon = R.drawable.ic_search,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          value = "",
          onValueChange = {},
          label = "Label",
          trailingIcon = R.drawable.ic_cross,
          leadingIcon = R.drawable.ic_search,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          value = "Input",
          onValueChange = {},
          label = "Label",
          supportingText = "Supporting text",
        )
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewClerkTextFieldError() {
  ClerkMaterialTheme {
    Column(
      modifier = Modifier.background(color = ClerkMaterialTheme.colors.background).padding(dp12),
      verticalArrangement = Arrangement.spacedBy(dp20, alignment = Alignment.CenterVertically),
    ) {
      ClerkTextField(
        value = "Input",
        onValueChange = {},
        label = "Label",
        trailingIcon = R.drawable.ic_cross,
        supportingText = "Supporting text",
        isError = true,
      )

      ClerkTextField(
        value = "",
        onValueChange = {},
        label = "Label",
        trailingIcon = R.drawable.ic_cross,
        supportingText = "Supporting text",
        isError = true,
      )

      ClerkTextField(
        value = "Input",
        onValueChange = {},
        placeholder = "Placeholder",
        label = "Label",
        supportingText = "Supporting text",
        isError = true,
      )

      ClerkTextField(
        value = "",
        onValueChange = {},
        label = "Label",
        supportingText = "Supporting text",
        leadingIcon = R.drawable.ic_search,
        isError = true,
      )

      ClerkTextField(
        value = "Input",
        onValueChange = {},
        label = "Label",
        trailingIcon = R.drawable.ic_cross,
        leadingIcon = R.drawable.ic_search,
        supportingText = "Supporting text",
        isError = true,
      )

      ClerkTextField(
        value = "",
        onValueChange = {},
        label = "Label",
        trailingIcon = R.drawable.ic_cross,
        leadingIcon = R.drawable.ic_search,
        supportingText = "Supporting text",
        isError = true,
      )

      ClerkTextField(
        value = "Input",
        onValueChange = {},
        label = "Label",
        supportingText = "Supporting text",
        isError = true,
      )
    }
  }
}
