package com.clerk.ui.userprofile.custom

internal val UserProfileRow.section: UserProfileSection
  get() =
    when (this) {
      UserProfileRow.ManageAccount,
      UserProfileRow.Security -> UserProfileSection.Profile
      UserProfileRow.SignOut -> UserProfileSection.Account
    }

internal val UserProfileCustomRowPlacement.section: UserProfileSection
  get() =
    when (this) {
      is UserProfileCustomRowPlacement.SectionStart -> section
      is UserProfileCustomRowPlacement.SectionEnd -> section
      is UserProfileCustomRowPlacement.Before -> row.section
      is UserProfileCustomRowPlacement.After -> row.section
    }

/**
 * Returns the effective list of custom rows to render. Custom rows are only shown when a
 * [hasDestination] is `true`; otherwise an empty list is returned to prevent navigation to
 * unregistered destinations.
 */
internal fun effectiveCustomRows(
  customRows: List<UserProfileCustomRow>,
  hasDestination: Boolean,
): List<UserProfileCustomRow> = if (hasDestination) customRows else emptyList()

/**
 * Merges [customRows] into [builtInRows] for the given [section], respecting each custom row's
 * [UserProfileCustomRowPlacement].
 *
 * The algorithm processes rows in this order:
 * 1. [UserProfileCustomRowPlacement.SectionStart] rows
 * 2. For each built-in row: [UserProfileCustomRowPlacement.Before] rows, the built-in row itself,
 *    then [UserProfileCustomRowPlacement.After] rows
 * 3. [UserProfileCustomRowPlacement.SectionEnd] rows
 *
 * Ordering within each placement group preserves the original list order from [customRows].
 */
internal fun buildRenderedRows(
  builtInRows: List<UserProfileRow>,
  section: UserProfileSection,
  customRows: List<UserProfileCustomRow>,
): List<UserProfileListRow> {
  val sectionCustomRows = customRows.filter { it.placement.section == section }

  val sectionStartRows =
    sectionCustomRows.filter { it.placement is UserProfileCustomRowPlacement.SectionStart }
  val sectionEndRows =
    sectionCustomRows.filter { it.placement is UserProfileCustomRowPlacement.SectionEnd }
  val beforeMap =
    sectionCustomRows
      .filter { it.placement is UserProfileCustomRowPlacement.Before }
      .groupBy { (it.placement as UserProfileCustomRowPlacement.Before).row }
  val afterMap =
    sectionCustomRows
      .filter { it.placement is UserProfileCustomRowPlacement.After }
      .groupBy { (it.placement as UserProfileCustomRowPlacement.After).row }

  return buildList {
    addAll(sectionStartRows.map { UserProfileListRow.Custom(it) })
    for (builtIn in builtInRows) {
      addAll(beforeMap[builtIn].orEmpty().map { UserProfileListRow.Custom(it) })
      add(UserProfileListRow.BuiltIn(builtIn))
      addAll(afterMap[builtIn].orEmpty().map { UserProfileListRow.Custom(it) })
    }
    addAll(sectionEndRows.map { UserProfileListRow.Custom(it) })
  }
}
