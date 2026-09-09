package com.clerk.ui.core.common

import android.util.Log

internal object ClerkLog {
  fun d(message: String) {
    Log.d("ClerkUI", message)
  }

  fun v(message: String) {
    Log.v("ClerkUI", message)
  }

  fun e(message: String) {
    Log.e("ClerkUI", message)
  }

  fun w(message: String) {
    Log.w("ClerkUI", message)
  }
}
