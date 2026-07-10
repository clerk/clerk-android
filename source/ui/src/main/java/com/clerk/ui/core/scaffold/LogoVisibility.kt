package com.clerk.ui.core.scaffold

internal fun shouldShowInstanceLogo(
  hasLogo: Boolean,
  organizationLogoUrl: String?,
  hasCustomLogo: Boolean = false,
): Boolean {
  return hasLogo && (hasCustomLogo || !organizationLogoUrl.isNullOrBlank())
}
