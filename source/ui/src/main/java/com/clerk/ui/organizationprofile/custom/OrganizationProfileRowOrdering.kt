package com.clerk.ui.organizationprofile.custom

internal val OrganizationProfileRow.section: OrganizationProfileSection
  get() =
    when (this) {
      OrganizationProfileRow.Members,
      OrganizationProfileRow.VerifiedDomains -> OrganizationProfileSection.Profile
      OrganizationProfileRow.LeaveOrganization,
      OrganizationProfileRow.DeleteOrganization -> OrganizationProfileSection.Actions
    }

internal val OrganizationProfileCustomRowPlacement.section: OrganizationProfileSection
  get() =
    when (this) {
      is OrganizationProfileCustomRowPlacement.SectionStart -> section
      is OrganizationProfileCustomRowPlacement.SectionEnd -> section
      is OrganizationProfileCustomRowPlacement.Before -> row.section
      is OrganizationProfileCustomRowPlacement.After -> row.section
    }

/**
 * Returns the effective list of custom rows to render. Custom rows are only shown when a
 * destination exists so rows cannot navigate to unregistered screens.
 */
internal fun effectiveOrganizationProfileCustomRows(
  customRows: List<OrganizationProfileCustomRow>,
  hasDestination: Boolean,
): List<OrganizationProfileCustomRow> = if (hasDestination) customRows else emptyList()

/**
 * Merges [customRows] into [builtInRows] for the given [section], respecting each row placement.
 */
internal fun buildOrganizationProfileRenderedRows(
  builtInRows: List<OrganizationProfileRow>,
  section: OrganizationProfileSection,
  customRows: List<OrganizationProfileCustomRow>,
): List<OrganizationProfileListRow> {
  val sectionCustomRows = customRows.filter { it.placement.section == section }
  val sectionStartRows =
    sectionCustomRows.filter { it.placement is OrganizationProfileCustomRowPlacement.SectionStart }
  val sectionEndRows =
    sectionCustomRows.filter { it.placement is OrganizationProfileCustomRowPlacement.SectionEnd }
  val beforeMap =
    sectionCustomRows
      .filter { it.placement is OrganizationProfileCustomRowPlacement.Before }
      .groupBy { (it.placement as OrganizationProfileCustomRowPlacement.Before).row }
  val afterMap =
    sectionCustomRows
      .filter { it.placement is OrganizationProfileCustomRowPlacement.After }
      .groupBy { (it.placement as OrganizationProfileCustomRowPlacement.After).row }

  return buildList {
    addAll(sectionStartRows.map { OrganizationProfileListRow.Custom(it) })
    for (builtIn in builtInRows) {
      addAll(beforeMap[builtIn].orEmpty().map { OrganizationProfileListRow.Custom(it) })
      add(OrganizationProfileListRow.BuiltIn(builtIn))
      addAll(afterMap[builtIn].orEmpty().map { OrganizationProfileListRow.Custom(it) })
    }
    addAll(sectionEndRows.map { OrganizationProfileListRow.Custom(it) })
  }
}
