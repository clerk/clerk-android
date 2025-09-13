@file:SuppressLint("ComposeModifierMissing")

package com.clerk.ui.core.common

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.ui.core.common.dimens.dp16
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.common.dimens.dp32
import com.clerk.ui.core.common.dimens.dp8

internal object Spacers {

  internal object Vertical {

    @Composable
    internal fun Spacer8() {
      Spacer(modifier = Modifier.height(dp8))
    }

    @Composable
    internal fun Spacer16() {
      Spacer(modifier = Modifier.height(dp16))
    }

    @Composable
    internal fun Spacer24() {
      Spacer(modifier = Modifier.height(dp24))
    }

    @Composable
    internal fun Spacer32() {
      Spacer(modifier = Modifier.height(dp32))
    }
  }
}
