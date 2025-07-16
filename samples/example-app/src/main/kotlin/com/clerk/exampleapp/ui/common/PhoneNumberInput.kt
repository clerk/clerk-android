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
import com.clerk.exampleapp.utils.NanpVisualTransformation

@Composable
fun PhoneNumberInput(
  phoneNumber: String,
  modifier: Modifier = Modifier,
  onValueChange: (String) -> Unit,
) {
  OutlinedTextField(
    modifier =
      Modifier.fillMaxWidth()
        .padding(top = 32.dp)
        .semantics { ContentType.PhoneNumber }
        .then(modifier),
    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
    value = phoneNumber,
    onValueChange = { if (it.length <= 10) onValueChange(it) },
    visualTransformation = NanpVisualTransformation(),
    label = { Text(text = stringResource(R.string.enter_your_phone_number)) },
    shape = RoundedCornerShape(8.dp),
    colors =
      OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = primaryLight,
        focusedBorderColor = primaryLight,
        focusedLabelColor = primaryLight,
      ),
  )
}
