package com.clerk.exampleapp.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clerk.exampleapp.R
import com.clerk.exampleapp.ui.theme.primaryLight

@Composable
fun CodeInput(value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
  OutlinedTextField(
    modifier =
      Modifier.fillMaxWidth()
        .padding(top = 32.dp)
        .semantics { ContentType.SmsOtpCode }
        .then(modifier),
    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
    value = value,
    onValueChange = { onValueChange(it) },
    label = { Text(text = stringResource(R.string.enter_your_code)) },
    shape = RoundedCornerShape(8.dp),
    colors =
      OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = primaryLight,
        focusedBorderColor = primaryLight,
        focusedLabelColor = primaryLight,
      ),
  )
}
