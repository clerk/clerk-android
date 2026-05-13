package com.clerk.ui.organizationprofile.custom

internal sealed interface OrganizationProfileListRow {
  data class BuiltIn(val row: OrganizationProfileRow) : OrganizationProfileListRow

  data class Custom(val customRow: OrganizationProfileCustomRow) : OrganizationProfileListRow
}
