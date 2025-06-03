@file:Suppress("unused", "FunctionNaming")

package com.clerk.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object Spacers {
  object Vertical {
    @Composable
    fun Spacer_8(modifier: Modifier = Modifier) =
      Spacer(modifier = Modifier.height(8.dp).then(modifier))

    @Composable
    fun Spacer_16(modifier: Modifier = Modifier) =
      Spacer(modifier = Modifier.height(16.dp).then(modifier))

    @Composable
    fun Spacer_24(modifier: Modifier = Modifier) =
      Spacer(modifier = Modifier.height(24.dp).then(modifier))

    @Composable
    fun Spacer_32(modifier: Modifier = Modifier) =
      Spacer(modifier = Modifier.height(32.dp).then(modifier))
  }

  object Horizontal {
    @Composable
    fun Spacer_6(modifier: Modifier = Modifier) =
      Spacer(modifier = Modifier.width(6.dp).then(modifier))

    @Composable
    fun Spacer_12(modifier: Modifier = Modifier) =
      Spacer(modifier = Modifier.width(12.dp).then(modifier))

    @Composable
    fun Spacer_16(modifier: Modifier = Modifier) =
      Spacer(modifier = Modifier.width(16.dp).then(modifier))
  }
}
