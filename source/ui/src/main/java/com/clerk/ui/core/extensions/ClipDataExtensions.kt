package com.clerk.ui.core.extensions

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.os.PersistableBundle

// EXTRA_IS_SENSITIVE is a String constant inlined at compile time; releases before API 33 ignore
// the extra.
@SuppressLint("InlinedApi")
internal fun ClipData.markSensitive(): ClipData = apply {
  description.extras =
    PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
}
