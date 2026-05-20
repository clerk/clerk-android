package com.clerk.ui.organizationprofile.custom

import kotlin.test.Test
import kotlin.test.assertEquals

class OrganizationProfileRowOrderingTest {

  private fun customRow(routeKey: String, placement: OrganizationProfileCustomRowPlacement) =
    OrganizationProfileCustomRow(
      routeKey = routeKey,
      title = routeKey,
      icon = OrganizationProfileRowIcon.Resource(0),
      placement = placement,
    )

  @Test
  fun `returns only built-in rows when no custom rows are provided`() {
    val rows =
      buildOrganizationProfileRenderedRows(
        builtInRows =
          listOf(OrganizationProfileRow.Members, OrganizationProfileRow.VerifiedDomains),
        section = OrganizationProfileSection.Profile,
        customRows = emptyList(),
      )

    assertEquals(
      listOf(
        OrganizationProfileListRow.BuiltIn(OrganizationProfileRow.Members),
        OrganizationProfileListRow.BuiltIn(OrganizationProfileRow.VerifiedDomains),
      ),
      rows,
    )
  }

  @Test
  fun `section start rows appear before built-in rows`() {
    val custom =
      customRow(
        routeKey = "billing",
        placement =
          OrganizationProfileCustomRowPlacement.SectionStart(OrganizationProfileSection.Profile),
      )

    val rows =
      buildOrganizationProfileRenderedRows(
        builtInRows = listOf(OrganizationProfileRow.Members),
        section = OrganizationProfileSection.Profile,
        customRows = listOf(custom),
      )

    assertEquals(
      listOf(
        OrganizationProfileListRow.Custom(custom),
        OrganizationProfileListRow.BuiltIn(OrganizationProfileRow.Members),
      ),
      rows,
    )
  }

  @Test
  fun `before and after rows are placed around anchors`() {
    val beforeDomains =
      customRow(
        routeKey = "beforeDomains",
        placement =
          OrganizationProfileCustomRowPlacement.Before(OrganizationProfileRow.VerifiedDomains),
      )
    val afterDomains =
      customRow(
        routeKey = "afterDomains",
        placement =
          OrganizationProfileCustomRowPlacement.After(OrganizationProfileRow.VerifiedDomains),
      )

    val rows =
      buildOrganizationProfileRenderedRows(
        builtInRows =
          listOf(OrganizationProfileRow.Members, OrganizationProfileRow.VerifiedDomains),
        section = OrganizationProfileSection.Profile,
        customRows = listOf(beforeDomains, afterDomains),
      )

    assertEquals(
      listOf(
        OrganizationProfileListRow.BuiltIn(OrganizationProfileRow.Members),
        OrganizationProfileListRow.Custom(beforeDomains),
        OrganizationProfileListRow.BuiltIn(OrganizationProfileRow.VerifiedDomains),
        OrganizationProfileListRow.Custom(afterDomains),
      ),
      rows,
    )
  }

  @Test
  fun `custom rows with the same placement keep declaration order`() {
    val billing =
      customRow(
        routeKey = "billing",
        placement = OrganizationProfileCustomRowPlacement.After(OrganizationProfileRow.Members),
      )
    val preferences =
      customRow(
        routeKey = "preferences",
        placement = OrganizationProfileCustomRowPlacement.After(OrganizationProfileRow.Members),
      )

    val rows =
      buildOrganizationProfileRenderedRows(
        builtInRows = listOf(OrganizationProfileRow.Members),
        section = OrganizationProfileSection.Profile,
        customRows = listOf(billing, preferences),
      )

    assertEquals(
      listOf(
        OrganizationProfileListRow.BuiltIn(OrganizationProfileRow.Members),
        OrganizationProfileListRow.Custom(billing),
        OrganizationProfileListRow.Custom(preferences),
      ),
      rows,
    )
  }

  @Test
  fun `filters custom rows to their section`() {
    val actionRow =
      customRow(
        routeKey = "action",
        placement =
          OrganizationProfileCustomRowPlacement.SectionEnd(OrganizationProfileSection.Actions),
      )

    val rows =
      buildOrganizationProfileRenderedRows(
        builtInRows = listOf(OrganizationProfileRow.Members),
        section = OrganizationProfileSection.Profile,
        customRows = listOf(actionRow),
      )

    assertEquals(listOf(OrganizationProfileListRow.BuiltIn(OrganizationProfileRow.Members)), rows)
  }

  @Test
  fun `custom rows are hidden without a custom destination`() {
    val customRows =
      listOf(
        customRow(
          routeKey = "billing",
          placement = OrganizationProfileCustomRowPlacement.After(OrganizationProfileRow.Members),
        )
      )

    assertEquals(
      customRows,
      effectiveOrganizationProfileCustomRows(customRows, hasDestination = true),
    )
    assertEquals(
      emptyList(),
      effectiveOrganizationProfileCustomRows(customRows, hasDestination = false),
    )
  }
}
