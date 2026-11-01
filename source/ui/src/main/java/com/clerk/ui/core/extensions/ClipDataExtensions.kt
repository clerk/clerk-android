package com.clerk.ui.core.extensions

import android.content.ClipData
import android.content.ClipDescription
import android.os.PersistableBundle

internal fun ClipData.markSensitive(): ClipData = apply {
  description.extras =
    PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
}
