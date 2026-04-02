package com.clerk.ui.userprofile.custom

import kotlin.test.Test
import kotlin.test.assertEquals

class UserProfileRowOrderingTest {

  private fun customRow(
    routeKey: String,
    placement: UserProfileCustomRowPlacement,
  ) =
    UserProfileCustomRow(
      routeKey = routeKey,
      title = routeKey,
      icon = UserProfileRowIcon.Resource(0),
      placement = placement,
    )

  @Test
  fun `returns only built-in rows when no custom rows are provided`() {
    val rows =
      buildRenderedRows(
        builtInRows = listOf(UserProfileRow.ManageAccount, UserProfileRow.Security),
        section = UserProfileSection.Profile,
        customRows = emptyList(),
      )

    assertEquals(
      listOf(
        UserProfileListRow.BuiltIn(UserProfileRow.ManageAccount),
        UserProfileListRow.BuiltIn(UserProfileRow.Security),
      ),
      rows,
    )
  }

  @Test
  fun `sectionStart rows appear before all built-in rows`() {
    val custom = customRow("start", UserProfileCustomRowPlacement.SectionStart(UserProfileSection.Profile))
    val rows =
      buildRenderedRows(
        builtInRows = listOf(UserProfileRow.ManageAccount, UserProfileRow.Security),
        section = UserProfileSection.Profile,
        customRows = listOf(custom),
      )

    assertEquals(UserProfileListRow.Custom(custom), rows.first())
    assertEquals(UserProfileListRow.BuiltIn(UserProfileRow.ManageAccount), rows[1])
  }

  @Test
  fun `sectionEnd rows appear after all built-in rows`() {
    val custom = customRow("end", UserProfileCustomRowPlacement.SectionEnd(UserProfileSection.Profile))
    val rows =
      buildRenderedRows(
        builtInRows = listOf(UserProfileRow.ManageAccount, UserProfileRow.Security),
        section = UserProfileSection.Profile,
        customRows = listOf(custom),
      )

    assertEquals(UserProfileListRow.Custom(custom), rows.last())
    assertEquals(UserProfileListRow.BuiltIn(UserProfileRow.Security), rows[rows.size - 2])
  }

  @Test
  fun `before placement inserts custom row before the anchor`() {
    val custom = customRow("beforeSec", UserProfileCustomRowPlacement.Before(UserProfileRow.Security))
    val rows =
      buildRenderedRows(
        builtInRows = listOf(UserProfileRow.ManageAccount, UserProfileRow.Security),
        section = UserProfileSection.Profile,
        customRows = listOf(custom),
      )

    assertEquals(
      listOf(
        UserProfileListRow.BuiltIn(UserProfileRow.ManageAccount),
        UserProfileListRow.Custom(custom),
        UserProfileListRow.BuiltIn(UserProfileRow.Security),
      ),
      rows,
    )
  }

  @Test
  fun `after placement inserts custom row after the anchor`() {
    val custom = customRow("afterManage", UserProfileCustomRowPlacement.After(UserProfileRow.ManageAccount))
    val rows =
      buildRenderedRows(
        builtInRows = listOf(UserProfileRow.ManageAccount, UserProfileRow.Security),
        section = UserProfileSection.Profile,
        customRows = listOf(custom),
      )

    assertEquals(
      listOf(
        UserProfileListRow.BuiltIn(UserProfileRow.ManageAccount),
        UserProfileListRow.Custom(custom),
        UserProfileListRow.BuiltIn(UserProfileRow.Security),
      ),
      rows,
    )
  }

  @Test
  fun `filters out custom rows that belong to a different section`() {
    val accountRow =
      customRow("accountOnly", UserProfileCustomRowPlacement.SectionEnd(UserProfileSection.Account))
    val rows =
      buildRenderedRows(
        builtInRows = listOf(UserProfileRow.ManageAccount, UserProfileRow.Security),
        section = UserProfileSection.Profile,
        customRows = listOf(accountRow),
      )

    assertEquals(
      listOf(
        UserProfileListRow.BuiltIn(UserProfileRow.ManageAccount),
        UserProfileListRow.BuiltIn(UserProfileRow.Security),
      ),
      rows,
    )
  }

  @Test
  fun `preserves input order within the same placement bucket`() {
    val first = customRow("first", UserProfileCustomRowPlacement.After(UserProfileRow.Security))
    val second = customRow("second", UserProfileCustomRowPlacement.After(UserProfileRow.Security))
    val third = customRow("third", UserProfileCustomRowPlacement.After(UserProfileRow.Security))
    val rows =
      buildRenderedRows(
        builtInRows = listOf(UserProfileRow.ManageAccount, UserProfileRow.Security),
        section = UserProfileSection.Profile,
        customRows = listOf(first, second, third),
      )

    assertEquals(
      listOf(
        UserProfileListRow.BuiltIn(UserProfileRow.ManageAccount),
        UserProfileListRow.BuiltIn(UserProfileRow.Security),
        UserProfileListRow.Custom(first),
        UserProfileListRow.Custom(second),
        UserProfileListRow.Custom(third),
      ),
      rows,
    )
  }

  @Test
  fun `mixed placements produce correct ordering`() {
    val start = customRow("start", UserProfileCustomRowPlacement.SectionStart(UserProfileSection.Profile))
    val beforeSec = customRow("beforeSec", UserProfileCustomRowPlacement.Before(UserProfileRow.Security))
    val afterSec = customRow("afterSec", UserProfileCustomRowPlacement.After(UserProfileRow.Security))
    val end = customRow("end", UserProfileCustomRowPlacement.SectionEnd(UserProfileSection.Profile))
    val rows =
      buildRenderedRows(
        builtInRows = listOf(UserProfileRow.ManageAccount, UserProfileRow.Security),
        section = UserProfileSection.Profile,
        customRows = listOf(start, beforeSec, afterSec, end),
      )

    assertEquals(
      listOf(
        UserProfileListRow.Custom(start),
        UserProfileListRow.BuiltIn(UserProfileRow.ManageAccount),
        UserProfileListRow.Custom(beforeSec),
        UserProfileListRow.BuiltIn(UserProfileRow.Security),
        UserProfileListRow.Custom(afterSec),
        UserProfileListRow.Custom(end),
      ),
      rows,
    )
  }

  @Test
  fun `account section only includes sign out and matching custom rows`() {
    val beforeSignOut =
      customRow("beforeSignOut", UserProfileCustomRowPlacement.Before(UserProfileRow.SignOut))
    val profileRow =
      customRow("profileOnly", UserProfileCustomRowPlacement.SectionEnd(UserProfileSection.Profile))
    val rows =
      buildRenderedRows(
        builtInRows = listOf(UserProfileRow.SignOut),
        section = UserProfileSection.Account,
        customRows = listOf(beforeSignOut, profileRow),
      )

    assertEquals(
      listOf(
        UserProfileListRow.Custom(beforeSignOut),
        UserProfileListRow.BuiltIn(UserProfileRow.SignOut),
      ),
      rows,
    )
  }
}
