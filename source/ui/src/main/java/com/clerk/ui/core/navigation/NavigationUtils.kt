package com.clerk.ui.core.navigation

internal fun <T> MutableList<T>.pop(count: Int = 1) {
  repeat(count) {
    if (this.isNotEmpty()) {
      this.removeLastOrNull()
    } else {
      return
    }
  }
}
