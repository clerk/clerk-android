package com.clerk.ui.core.extensions

private val emailRegex =
  Regex("^[a-zA-Z0-9._%+-]+@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$")

internal val String.isEmailAddress: Boolean
  get() = emailRegex.matches(this)
