package com.clerk.ui.core.appbar

internal fun resolveBackAction(
  hasBackButton: Boolean,
  usesHostBackAction: Boolean,
  onBackPressed: () -> Unit,
  hostBackAction: (() -> Unit)?,
): (() -> Unit)? =
  when {
    hasBackButton -> onBackPressed
    usesHostBackAction -> hostBackAction
    else -> null
  }
