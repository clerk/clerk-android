package com.clerk.ui.organizationprofile.domains

import com.clerk.api.organizations.OrganizationDomain

internal sealed interface OrganizationVerifiedDomainsFlow {
  data object DomainsList : OrganizationVerifiedDomainsFlow

  data object AddDomain : OrganizationVerifiedDomainsFlow

  data class VerifyEmail(val domain: OrganizationDomain) : OrganizationVerifiedDomainsFlow

  data class VerifyCode(val domain: OrganizationDomain, val emailAddress: String) :
    OrganizationVerifiedDomainsFlow

  data class EnrollmentMode(val domain: OrganizationDomain) : OrganizationVerifiedDomainsFlow

  data class DeleteDomain(val domain: OrganizationDomain) : OrganizationVerifiedDomainsFlow
}

internal data class OrganizationVerifiedDomainsState(
  val domainsEnabled: Boolean = false,
  val canReadDomains: Boolean = false,
  val canManageDomains: Boolean = false,
  val isLoadingInitial: Boolean = false,
  val errorMessage: String? = null,
  val domains: List<OrganizationDomain> = emptyList(),
  val totalCount: Int = 0,
  val hasNextPage: Boolean = false,
  val isLoadingMore: Boolean = false,
  val activeMutationId: String? = null,
  val flow: OrganizationVerifiedDomainsFlow = OrganizationVerifiedDomainsFlow.DomainsList,
  val domainName: String = "",
  val affiliationEmailLocalPart: String = "",
  val verificationCode: String = "",
  val enrollmentModeOptions: List<OrganizationDomain.EnrollmentMode> = defaultEnrollmentModeOptions,
  val selectedEnrollmentMode: OrganizationDomain.EnrollmentMode =
    OrganizationDomain.EnrollmentMode.ManualInvitation,
) {
  val canLoadDomains: Boolean
    get() = domainsEnabled && (canReadDomains || canManageDomains)

  val canCreateDomain: Boolean
    get() = canManageDomains && domainName.trim().isNotEmpty() && activeMutationId == null

  val canSendVerificationEmail: Boolean
    get() =
      canManageDomains && affiliationEmailLocalPart.trim().isNotEmpty() && activeMutationId == null

  val canVerifyCode: Boolean
    get() = canManageDomains && verificationCode.trim().isNotEmpty() && activeMutationId == null

  val canUpdateEnrollmentMode: Boolean
    get() =
      canManageDomains &&
        selectedEnrollmentMode in enrollmentModeOptions &&
        activeMutationId == null
}

internal data class OrganizationVerifiedDomainsActions(
  val onRetry: () -> Unit,
  val onLoadMore: () -> Unit,
  val onShowAddDomain: () -> Unit,
  val onShowVerifyEmail: (OrganizationDomain) -> Unit,
  val onShowEnrollmentMode: (OrganizationDomain) -> Unit,
  val onShowDeleteDomain: (OrganizationDomain) -> Unit,
  val onDismissFlow: () -> Unit,
  val onDomainNameChanged: (String) -> Unit,
  val onCreateDomain: () -> Unit,
  val onAffiliationEmailLocalPartChanged: (String) -> Unit,
  val onSendAffiliationEmail: (OrganizationDomain) -> Unit,
  val onVerificationCodeChanged: (String) -> Unit,
  val onVerifyCode: (OrganizationDomain) -> Unit,
  val onResendVerificationCode: (OrganizationDomain, String) -> Unit,
  val onSelectEnrollmentMode: (OrganizationDomain.EnrollmentMode) -> Unit,
  val onUpdateEnrollmentMode: (OrganizationDomain) -> Unit,
  val onDeleteDomain: (OrganizationDomain) -> Unit,
)

internal val defaultEnrollmentModeOptions =
  listOf(
    OrganizationDomain.EnrollmentMode.ManualInvitation,
    OrganizationDomain.EnrollmentMode.AutomaticInvitation,
    OrganizationDomain.EnrollmentMode.AutomaticSuggestion,
  )

internal fun enrollmentModeOptions(
  rawModes: List<String>
): List<OrganizationDomain.EnrollmentMode> {
  val options =
    rawModes.mapNotNull { rawMode ->
      when (val mode = OrganizationDomain.EnrollmentMode.fromValue(rawMode)) {
        is OrganizationDomain.EnrollmentMode.Unknown -> null
        else -> mode
      }
    }
  return (defaultEnrollmentModeOptions + options).distinct()
}

internal const val CREATE_DOMAIN_MUTATION_ID = "create-domain"
internal const val SEND_CODE_MUTATION_ID = "send-code"
internal const val VERIFY_CODE_MUTATION_ID = "verify-code"
internal const val UPDATE_ENROLLMENT_MUTATION_ID = "update-enrollment"
internal const val DELETE_DOMAIN_MUTATION_ID = "delete-domain"

internal const val AFFILIATION_EMAIL_LOCAL_PART_MAX_LENGTH = 25
