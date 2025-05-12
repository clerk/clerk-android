package com.clerk.sdk.log

import android.util.Log

internal object ClerkLog {
  fun e(message: String) = Log.e("ClerkLog", "Clerk error: $message")

  fun w(message: String) = Log.w("ClerkLog", "Clerk warning: $message")

  fun i(message: String) = Log.i("ClerkLog", message)

  fun d(message: String) = Log.d("ClerkLog", message)

  fun v(message: String) = Log.v("ClerkLog", message)
}
