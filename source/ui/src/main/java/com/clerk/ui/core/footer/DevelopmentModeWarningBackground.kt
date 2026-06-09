package com.clerk.ui.core.footer

import androidx.annotation.DrawableRes
import com.clerk.ui.R

internal enum class DevelopmentModeWarningBackground(@param:DrawableRes val drawableResId: Int?) {
  Grey(R.drawable.dev_mode_background_grey),
  White(R.drawable.dev_mode_background_white),
  Themed(null),
}
