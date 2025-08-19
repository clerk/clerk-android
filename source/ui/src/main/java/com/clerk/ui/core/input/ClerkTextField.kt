package com.clerk.ui.core.input

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.LocalClerkColors
import com.clerk.ui.theme.LocalClerkDesign
import com.clerk.ui.theme.LocalComputedColors

@Composable
fun ClerkTextField(
  modifier: Modifier = Modifier,
  @DrawableRes leadingIcon: Int? = null,
  @DrawableRes trailingIcon: Int? = null,
  label: String? = null,
  placeholder: String? = null,
  input: String? = null,
  supportingText: String? = null,
  isError: Boolean = false,
  onLeadingIconClick: () -> Unit = {},
  onTrailingIconClick: () -> Unit = {},
) {
  var value by remember { mutableStateOf(input.orEmpty()) }
  val colors = LocalClerkColors.current
  val computedColors = LocalComputedColors.current
  val design = LocalClerkDesign.current
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  ClerkMaterialTheme {
    OutlinedTextField(
      interactionSource = interactionSource,
      modifier = Modifier
        .fillMaxWidth()
        .then(modifier),
      value = value,
      shape = RoundedCornerShape(design.borderRadius),
      isError = isError,
      colors =
        OutlinedTextFieldDefaults.colors(
          unfocusedBorderColor = computedColors.inputBorder,
          unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
          unfocusedContainerColor = MaterialTheme.colorScheme.background,
          focusedContainerColor = MaterialTheme.colorScheme.background,
          errorBorderColor = MaterialTheme.colorScheme.error,
          errorSupportingTextColor = MaterialTheme.colorScheme.error,
        ),
      leadingIcon =
        leadingIcon?.let { resId ->
          {
            Icon(
              modifier = Modifier
                .size(dp24)
                .clickable { onLeadingIconClick() },
              painter = painterResource(resId),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
      trailingIcon = {
        if (trailingIcon != null || isError) {
          val resId = trailingIcon ?: R.drawable.ic_warning
          val trailingIconColor =
            if (trailingIcon != null) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error
          Icon(
            modifier = Modifier
              .size(dp24)
              .clickable { onTrailingIconClick() },
            painter = painterResource(resId),
            contentDescription = null,
            tint = trailingIconColor,
          )
        }
      },
      placeholder =
        placeholder?.let {
          { Text(placeholder) }
      },
      label = {
        label?.let { text ->
          val style =
            if (isFocused || value.isNotEmpty()) MaterialTheme.typography.bodySmall
            else MaterialTheme.typography.bodyLarge
          val color =
            if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
          Text(text = text, style = style, color = color)
        }
      },
      textStyle = MaterialTheme.typography.bodyLarge,
      onValueChange = { value = it },
      supportingText =
        supportingText?.let {
          {
            Text(
              modifier = Modifier.padding(top = dp4),
              text = it,
              style = MaterialTheme.typography.bodySmall,
              color =
                if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewClerkTextField() {
  ClerkMaterialTheme {
    LazyColumn(
      modifier = Modifier
        .background(color = MaterialTheme.colorScheme.background)
        .padding(dp12),
      verticalArrangement = Arrangement.spacedBy(dp20, alignment = Alignment.CenterVertically),
    ) {
      item {
        ClerkTextField(
          input = "Input",
          label = "Label",
          trailingIcon = R.drawable.ic_cross,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          label = "Label",
          trailingIcon = R.drawable.ic_cross,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          placeholder = "Placeholder",
          trailingIcon = R.drawable.ic_cross,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          placeholder = "Placeholder",
          label = "Label",
          isError = true,
          supportingText = "Error message",
        )
      }
      item {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        ClerkTextField(
          label = "Focused",
          placeholder = "Type here",
          supportingText = "Supporting text",
          modifier = Modifier.focusRequester(focusRequester),
        )
      }
      item {
        ClerkTextField(
          input = "Input",
          trailingIcon = R.drawable.ic_cross,
          leadingIcon = R.drawable.ic_search,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          placeholder = "Placeholder",
          trailingIcon = R.drawable.ic_cross,
          leadingIcon = R.drawable.ic_search,
          supportingText = "Supporting text",
        )
      }
      item { ClerkTextField(label = "Label", input = "Input", supportingText = "Supporting text") }
      item { ClerkTextField(label = "Label", supportingText = "Supporting text") }
      item {
        ClerkTextField(
          label = "Label",
          input = "Input",
          leadingIcon = R.drawable.ic_search,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          label = "Label",
          leadingIcon = R.drawable.ic_search,
          supportingText = "Supporting text",
        )
      }
      item {
        ClerkTextField(
          label = "Label",
          placeholder = "Placeholder",
          leadingIcon = R.drawable.ic_search,
          supportingText = "Supporting text",
        )
      }
    }
  }
}
