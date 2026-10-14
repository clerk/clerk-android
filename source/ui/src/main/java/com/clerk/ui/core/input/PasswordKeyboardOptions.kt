package com.clerk.ui.core.input

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

public val PasswordKeyboardOptions: KeyboardOptions =
  KeyboardOptions(
    keyboardType = KeyboardType.Password,
    imeAction = ImeAction.Done,
    autoCorrectEnabled = false,
  )
