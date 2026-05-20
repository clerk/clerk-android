package com.clerk.ui.organizationprofile.custom

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/** Icon for a custom organization profile row. */
@Immutable
sealed interface OrganizationProfileRowIcon {
  /**
   * A drawable resource icon.
   *
   * @param resId Drawable resource ID to render for the row.
   */
  data class Resource(@DrawableRes val resId: Int) : OrganizationProfileRowIcon

  /**
   * A Compose [ImageVector] icon.
   *
   * @param imageVector Vector image to render for the row.
   */
  data class Vector(val imageVector: ImageVector) : OrganizationProfileRowIcon
}

/**
 * Built-in rows in [com.clerk.ui.organizationprofile.OrganizationProfileView] that can be used as
 * placement anchors.
 */
enum class OrganizationProfileRow {
  /** Opens the organization members, invitations, and membership requests view. */
  Members,

  /** Opens the verified domains management view. */
  VerifiedDomains,

  /** Opens the leave organization confirmation view. */
  LeaveOrganization,

  /** Opens the delete organization confirmation view. */
  DeleteOrganization,
}

/** Root-level sections in [com.clerk.ui.organizationprofile.OrganizationProfileView]. */
enum class OrganizationProfileSection {
  /** Contains organization management rows such as members and verified domains. */
  Profile,

  /** Contains organization-level action rows such as leave and delete. */
  Actions,
}

/** Where to insert a custom row relative to built-in organization profile rows. */
@Immutable
sealed interface OrganizationProfileCustomRowPlacement {
  /**
   * Inserts the row before every built-in row in [section].
   *
   * @param section Section where the row should be inserted.
   */
  data class SectionStart(val section: OrganizationProfileSection) :
    OrganizationProfileCustomRowPlacement

  /**
   * Inserts the row after every built-in row in [section].
   *
   * @param section Section where the row should be inserted.
   */
  data class SectionEnd(val section: OrganizationProfileSection) :
    OrganizationProfileCustomRowPlacement

  /**
   * Inserts the row immediately before [row] when that built-in row is visible.
   *
   * @param row Built-in row used as the placement anchor.
   */
  data class Before(val row: OrganizationProfileRow) : OrganizationProfileCustomRowPlacement

  /**
   * Inserts the row immediately after [row] when that built-in row is visible.
   *
   * @param row Built-in row used as the placement anchor.
   */
  data class After(val row: OrganizationProfileRow) : OrganizationProfileCustomRowPlacement
}

/**
 * A custom row to display in [com.clerk.ui.organizationprofile.OrganizationProfileView].
 *
 * @param routeKey A string key that identifies the destination to navigate to when tapped. The same
 *   key is passed to the `customDestination` composable so it can render the appropriate screen.
 * @param title The row title text.
 * @param icon The row icon.
 * @param placement Where to insert relative to built-in rows. Defaults to the end of the profile
 *   section.
 */
@Immutable
data class OrganizationProfileCustomRow(
  val routeKey: String,
  val title: String,
  val icon: OrganizationProfileRowIcon,
  val placement: OrganizationProfileCustomRowPlacement =
    OrganizationProfileCustomRowPlacement.SectionEnd(OrganizationProfileSection.Profile),
)
