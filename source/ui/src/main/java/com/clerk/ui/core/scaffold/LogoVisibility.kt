package com.clerk.ui.core.scaffold

internal fun shouldShowInstanceLogo(hasLogo: Boolean, organizationLogoUrl: String?): Boolean {
  return hasLogo && !organizationLogoUrl.isNullOrBlank()
}
