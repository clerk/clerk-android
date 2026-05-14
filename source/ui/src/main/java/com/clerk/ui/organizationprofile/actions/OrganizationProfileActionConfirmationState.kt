package com.clerk.ui.organizationprofile.actions

import java.text.Normalizer

internal enum class OrganizationProfileConfirmationAction {
  LeaveOrganization,
  DeleteOrganization,
}

internal data class OrganizationProfileActionConfirmationState(
  val confirmationText: String = "",
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val isComplete: Boolean = false,
) {
  fun canSubmit(organizationName: String): Boolean {
    return !isLoading && organizationNameConfirmationMatches(confirmationText, organizationName)
  }
}

internal fun organizationNameConfirmationMatches(input: String, organizationName: String): Boolean {
  return input.confirmationNormalized() == organizationName.confirmationNormalized()
}

private fun String.confirmationNormalized(): String {
  return Normalizer.normalize(trim(), Normalizer.Form.NFKC)
    .replace('\u2018', '\'')
    .replace('\u2019', '\'')
    .replace('\u201B', '\'')
    .replace('\u201C', '"')
    .replace('\u201D', '"')
    .replace('\u201F', '"')
}
