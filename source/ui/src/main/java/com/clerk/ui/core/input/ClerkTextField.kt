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
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.common.dimens.dp12
import com.clerk.ui.core.common.dimens.dp20
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.common.dimens.dp4
import com.clerk.ui.theme.ClerkMaterialTheme

/**
 * A customizable text input field component following Clerk's design system.
 *
 * This composable provides a styled `OutlinedTextField` with support for leading/trailing icons,
 * labels, placeholders, supporting text, and error states. The component automatically handles
 * focus states, theming, and accessibility features.
 *
 * @param value The current text value of the input field
 * @param modifier Modifier to be applied to the text field
 * @param leadingIcon Optional drawable resource ID for an icon displayed at the start of the field
 * @param trailingIcon Optional drawable resource ID for an icon displayed at the end of the field.
 *   When [isError] is true, this will be overridden with a warning icon
 * @param label Optional text label displayed above the input field
 * @param placeholder Optional placeholder text shown when the field is empty
 * @param supportingText Optional supporting/helper text displayed below the input field
 * @param isError Whether the field should be displayed in an error state with error styling
 * @param enabled Whether the text field is enabled and accepts user input
 * @param inputContentType The content type for autofill hints, defaults to [ContentType.Username]
 */
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
  inputContentType: ContentType = ContentType.Username,
  visualTransformation: VisualTransformation = VisualTransformation.None,
) {
  var isVisible by remember {
    mutableStateOf(visualTransformation !is PasswordVisualTransformation)
  }
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  val textFieldColors = getTextFieldColors()

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
      onValueChange = { onValueChange(it) },
      enabled = enabled,
      shape = ClerkMaterialTheme.shape,
      isError = isError,
      colors = textFieldColors,
      visualTransformation =
        if (visualTransformation is PasswordVisualTransformation) {
          if (isVisible) VisualTransformation.None else PasswordVisualTransformation()
        } else {
          visualTransformation
        },
      leadingIcon =
        leadingIcon?.let { resId ->
          { ClickableIcon(resId = resId, onClick = {}, contentDescription = null) }
        },
      trailingIcon = {
        TrailingIcon(
          trailingIcon,
          isError,
          visualTransformation,
          onClick = {
            if (visualTransformation is PasswordVisualTransformation) {
              isVisible = !isVisible
            }
          },
        )
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
private fun TrailingIcon(
  trailingIcon: Int?,
  isError: Boolean,
  visualTransformation: VisualTransformation,
  onClick: () -> Unit,
) {

  if (trailingIcon != null || isError || visualTransformation is PasswordVisualTransformation) {
    val resId =
      when {
        isError -> R.drawable.ic_warning
        visualTransformation is PasswordVisualTransformation -> R.drawable.ic_show
        else -> trailingIcon!!
      }
    val tint =
      if (isError) ClerkMaterialTheme.colors.danger else ClerkMaterialTheme.colors.mutedForeground
    ClickableIcon(resId = resId, onClick = onClick, tint = tint, contentDescription = null)
  }
}

@Composable
private fun getTextFieldColors(): TextFieldColors =
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

/**
 * A clickable icon component used within the text field for leading and trailing icons.
 *
 * @param resId The drawable resource ID for the icon
 * @param onClick Callback triggered when the icon is clicked
 * @param tint The color tint to apply to the icon, defaults to muted foreground color
 * @param contentDescription Content description for accessibility support
 */
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
          onValueChange = {},
          value = "Input",
          label = "Label",
          trailingIcon = R.drawable.ic_cross,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          onValueChange = {},
          value = "",
          label = "Label",
          trailingIcon = R.drawable.ic_cross,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          onValueChange = {},
          value = "Input",
          placeholder = "Placeholder",
          label = "Label",
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          onValueChange = {},
          value = "",
          label = "Label",
          supportingText = "Supporting text",
          leadingIcon = R.drawable.ic_search,
        )
      }
      item {
        ClerkTextField(
          onValueChange = {},
          value = "Input",
          label = "Label",
          trailingIcon = R.drawable.ic_cross,
          leadingIcon = R.drawable.ic_search,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          onValueChange = {},
          value = "",
          label = "Label",
          trailingIcon = R.drawable.ic_cross,
          leadingIcon = R.drawable.ic_search,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          onValueChange = {},
          value = "Input",
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
        onValueChange = {},
        value = "Input",
        label = "Label",
        trailingIcon = R.drawable.ic_cross,
        supportingText = "Supporting text",
        isError = true,
      )

      ClerkTextField(
        onValueChange = {},
        value = "",
        label = "Label",
        trailingIcon = R.drawable.ic_cross,
        supportingText = "Supporting text",
        isError = true,
      )

      ClerkTextField(
        onValueChange = {},
        value = "Input",
        placeholder = "Placeholder",
        label = "Label",
        supportingText = "Supporting text",
        isError = true,
      )

      ClerkTextField(
        onValueChange = {},
        value = "",
        label = "Label",
        supportingText = "Supporting text",
        leadingIcon = R.drawable.ic_search,
        isError = true,
      )

      ClerkTextField(
        onValueChange = {},
        value = "Input",
        label = "Label",
        trailingIcon = R.drawable.ic_cross,
        leadingIcon = R.drawable.ic_search,
        supportingText = "Supporting text",
        isError = true,
      )

      ClerkTextField(
        onValueChange = {},
        value = "",
        label = "Label",
        trailingIcon = R.drawable.ic_cross,
        leadingIcon = R.drawable.ic_search,
        supportingText = "Supporting text",
        isError = true,
      )

      ClerkTextField(
        onValueChange = {},
        value = "Input",
        label = "Label",
        supportingText = "Supporting text",
        isError = true,
      )
    }
  }
}
