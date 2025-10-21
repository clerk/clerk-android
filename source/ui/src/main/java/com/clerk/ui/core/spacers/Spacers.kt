package com.clerk.ui.core.spacers

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp8

internal object Spacers {

  internal object Vertical {

    @Composable
    internal fun Spacer4() {
      Spacer(modifier = Modifier.Companion.height(dp4))
    }

    @Composable
    internal fun Spacer8() {
      Spacer(modifier = Modifier.Companion.height(dp8))
    }

    @Composable
    internal fun Spacer12() {
      Spacer(modifier = Modifier.Companion.height(dp12))
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

  internal object Horizontal {

    @Composable
    internal fun Spacer8() {
      Spacer(modifier = Modifier.Companion.width(dp8))
    }

    @Composable
    internal fun Spacer16() {
      Spacer(modifier = Modifier.Companion.width(dp16))
    }
  }
}
