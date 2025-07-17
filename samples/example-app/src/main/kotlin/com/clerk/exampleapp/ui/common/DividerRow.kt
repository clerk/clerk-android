package com.clerk.exampleapp.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DividerRow(modifier: Modifier = Modifier) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 32.dp).then(modifier),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      thickness = 1.dp,
      color = MaterialTheme.colorScheme.primary.copy(alpha = .2f),
    )

    Text(
      modifier = Modifier.padding(horizontal = 16.dp),
      text = "or",
      style = MaterialTheme.typography.titleSmall,
      color = Color.LightGray,
    )

    HorizontalDivider(
      modifier = Modifier.weight(1f),
      thickness = 1.dp,
      color = MaterialTheme.colorScheme.primary.copy(alpha = .2f),
    )
  }
}
