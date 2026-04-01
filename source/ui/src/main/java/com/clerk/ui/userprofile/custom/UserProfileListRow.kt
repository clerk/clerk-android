package com.clerk.ui.userprofile.custom

internal sealed interface UserProfileListRow {
  data class BuiltIn(val row: UserProfileRow) : UserProfileListRow

  data class Custom(val customRow: UserProfileCustomRow) : UserProfileListRow
}
