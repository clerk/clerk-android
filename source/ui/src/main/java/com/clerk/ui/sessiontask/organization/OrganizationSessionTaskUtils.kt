package com.clerk.ui.sessiontask.organization

internal fun createOrganizationSlug(name: String): String {
  return name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}

internal fun isValidOrganizationSlug(slug: String): Boolean {
  return Regex("^(?=.*[a-z0-9])[a-z0-9-]+$").matches(slug)
}
