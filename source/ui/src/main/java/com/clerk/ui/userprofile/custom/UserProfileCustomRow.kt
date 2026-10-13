package com.clerk.ui.userprofile.custom

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/** Icon for a custom user profile row. */
@Immutable
public sealed interface UserProfileRowIcon {
  /** A drawable resource icon. */
  public data class Resource(@DrawableRes val resId: Int) : UserProfileRowIcon

  /** A Compose [ImageVector] icon. */
  public data class Vector(val imageVector: ImageVector) : UserProfileRowIcon
}

/**
 * Built-in rows in [com.clerk.ui.userprofile.UserProfileView] that can be used as placement
 * anchors.
 */
public enum class UserProfileRow {
  ManageAccount,
  Security,
  SwitchAccount,
  AddAccount,
  SignOut,
}

/** Root-level sections in [com.clerk.ui.userprofile.UserProfileView]. */
public enum class UserProfileSection {
  /** Contains [UserProfileRow.ManageAccount] and [UserProfileRow.Security]. */
  Profile,

  /** Contains account switching and sign-out rows. */
  Account,
}

/** Where to insert a custom row relative to built-in rows. */
@Immutable
public sealed interface UserProfileCustomRowPlacement {
  public data class SectionStart(val section: UserProfileSection) : UserProfileCustomRowPlacement

  public data class SectionEnd(val section: UserProfileSection) : UserProfileCustomRowPlacement

  public data class Before(val row: UserProfileRow) : UserProfileCustomRowPlacement

  public data class After(val row: UserProfileRow) : UserProfileCustomRowPlacement
}

/**
 * A custom row to display in [com.clerk.ui.userprofile.UserProfileView].
 *
 * @param routeKey A string key that identifies the destination to navigate to when tapped. The same
 *   key is passed to the `customDestination` composable so it can render the appropriate screen.
 * @param title The title shown in the row and in its destination's navigation header.
 * @param icon The row icon.
 * @param placement Where to insert relative to built-in rows. Defaults to the end of the profile
 *   section.
 */
@Immutable
public data class UserProfileCustomRow(
  val routeKey: String,
  val title: String,
  val icon: UserProfileRowIcon,
  val placement: UserProfileCustomRowPlacement =
    UserProfileCustomRowPlacement.SectionEnd(UserProfileSection.Profile),
)
