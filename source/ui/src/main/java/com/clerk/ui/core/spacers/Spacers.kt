package com.clerk.ui.core.spacers

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp8

internal object Spacers {

  internal object Vertical {

    @Composable
    internal fun Spacer8() {
      Spacer(modifier = Modifier.Companion.height(dp8))
    }

    @Composable
    internal fun Spacer16() {
      Spacer(modifier = Modifier.Companion.height(dp16))
    }

    @Composable
    internal fun Spacer24() {
      Spacer(modifier = Modifier.Companion.height(dp24))
    }

    @Composable
    internal fun Spacer32() {
      Spacer(modifier = Modifier.Companion.height(dp32))
    }
  }
}
