@file:Suppress("TooManyFunctions")

package com.clerk.ui.organizationprofile.domains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationDomain
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.createDomain
import com.clerk.api.organizations.delete
import com.clerk.api.organizations.getDomains
import com.clerk.api.organizations.sendEmailCode
import com.clerk.api.organizations.updateEnrollmentMode
import com.clerk.api.organizations.verifyCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OrganizationVerifiedDomainsViewModel(
  private val pageSize: Int = DEFAULT_PAGE_SIZE,
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

  private val mutableState = MutableStateFlow(OrganizationVerifiedDomainsState())
  val state = mutableState.asStateFlow()

  private var organization: Organization? = null
  private var membership: OrganizationMembership? = null
  private var domainsEnabled: Boolean = false

  fun load(
    organization: Organization,
    membership: OrganizationMembership?,
    domainsEnabled: Boolean = Clerk.organizationDomainsIsEnabled,
    enrollmentModes: List<String> = Clerk.organizationDomainEnrollmentModes,
  ) {
    this.organization = organization
    this.membership = membership
    this.domainsEnabled = domainsEnabled

    val canReadDomains = membership?.canReadDomains == true
    val canManageDomains = membership?.canManageDomains == true
    val options = enrollmentModeOptions(enrollmentModes)
    val shouldLoad = domainsEnabled && (canReadDomains || canManageDomains)

    mutableState.value =
      mutableState.value.copy(
        domainsEnabled = domainsEnabled,
        canReadDomains = canReadDomains,
        canManageDomains = canManageDomains,
        enrollmentModeOptions = options,
        selectedEnrollmentMode =
          mutableState.value.selectedEnrollmentMode.takeIf { it in options } ?: options.first(),
        isLoadingInitial = shouldLoad,
        errorMessage = null,
      )

    if (!shouldLoad) {
      mutableState.value =
        mutableState.value.copy(
          isLoadingInitial = false,
          domains = emptyList(),
          totalCount = 0,
          hasNextPage = false,
        )
      return
    }

    viewModelScope.launch(dispatcher) {
      loadDomains(reset = true)
      mutableState.value = mutableState.value.copy(isLoadingInitial = false)
    }
  }

  fun retry() {
    val currentOrganization = organization ?: return
    load(
      organization = currentOrganization,
      membership = membership,
      domainsEnabled = domainsEnabled,
      enrollmentModes = Clerk.organizationDomainEnrollmentModes,
    )
  }

  fun loadMoreDomains() {
    val current = mutableState.value
    if (!current.hasNextPage || current.isLoadingMore) return
    viewModelScope.launch(dispatcher) { loadDomains(reset = false) }
  }

  fun showAddDomain() {
    if (!mutableState.value.canManageDomains) return
    mutableState.value =
      mutableState.value.copy(
        flow = OrganizationVerifiedDomainsFlow.AddDomain,
        domainName = "",
        errorMessage = null,
      )
  }

  fun showVerifyEmail(domain: OrganizationDomain) {
    if (!mutableState.value.canManageDomains) return
    mutableState.value =
      mutableState.value.copy(
        flow = OrganizationVerifiedDomainsFlow.VerifyEmail(domain),
        affiliationEmailLocalPart = domain.affiliationEmailAddress?.substringBefore("@").orEmpty(),
        verificationCode = "",
        errorMessage = null,
      )
  }

  fun showEnrollmentMode(domain: OrganizationDomain) {
    val current = mutableState.value
    if (!current.canManageDomains || !domain.isVerified) return
    val selected =
      domain.enrollmentModeType.takeIf {
        it !is OrganizationDomain.EnrollmentMode.Unknown && it in current.enrollmentModeOptions
      } ?: current.enrollmentModeOptions.first()
    mutableState.value =
      current.copy(
        flow = OrganizationVerifiedDomainsFlow.EnrollmentMode(domain),
        selectedEnrollmentMode = selected,
        deletePending = false,
        errorMessage = null,
      )
  }

  fun showDeleteDomain(domain: OrganizationDomain) {
    if (!mutableState.value.canManageDomains) return
    mutableState.value =
      mutableState.value.copy(
        flow = OrganizationVerifiedDomainsFlow.DeleteDomain(domain),
        errorMessage = null,
      )
  }

  fun dismissFlow() {
    mutableState.value =
      mutableState.value.copy(
        flow = OrganizationVerifiedDomainsFlow.DomainsList,
        domainName = "",
        affiliationEmailLocalPart = "",
        verificationCode = "",
        deletePending = false,
        activeMutationId = null,
        errorMessage = null,
      )
  }

  fun setDomainName(domainName: String) {
    mutableState.value = mutableState.value.copy(domainName = domainName)
  }

  fun createDomain() {
    val currentOrganization = organization ?: return
    val current = mutableState.value
    val name = current.domainName.trim()
    if (!current.canCreateDomain || name.isEmpty()) return

    mutableState.value =
      current.copy(activeMutationId = CREATE_DOMAIN_MUTATION_ID, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      when (val result = currentOrganization.createDomain(name = name)) {
        is ClerkResult.Success -> {
          upsertDomain(result.value)
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              domainName = "",
              affiliationEmailLocalPart =
                result.value.affiliationEmailAddress?.substringBefore("@").orEmpty(),
              flow =
                if (result.value.isVerified) OrganizationVerifiedDomainsFlow.DomainsList
                else OrganizationVerifiedDomainsFlow.VerifyEmail(result.value),
            )
        }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  fun setAffiliationEmailLocalPart(localPart: String) {
    mutableState.value =
      mutableState.value.copy(affiliationEmailLocalPart = localPart.sanitizedLocalPart())
  }

  fun sendAffiliationEmail(domain: OrganizationDomain) {
    val current = mutableState.value
    val localPart = current.affiliationEmailLocalPart.trim()
    if (!current.canSendVerificationEmail || localPart.isEmpty()) return
    val emailAddress = "$localPart@${domain.name}"

    sendEmailCode(domain = domain, emailAddress = emailAddress, nextFlow = true)
  }

  fun setVerificationCode(code: String) {
    mutableState.value = mutableState.value.copy(verificationCode = code)
  }

  fun verifyCode(domain: OrganizationDomain) {
    val current = mutableState.value
    val code = current.verificationCode.trim()
    if (!current.canVerifyCode || code.isEmpty()) return

    mutableState.value =
      current.copy(activeMutationId = VERIFY_CODE_MUTATION_ID, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      when (val result = domain.verifyCode(code = code)) {
        is ClerkResult.Success -> {
          upsertDomain(result.value)
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              verificationCode = "",
              flow = OrganizationVerifiedDomainsFlow.DomainsList,
            )
        }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  fun resendVerificationCode(domain: OrganizationDomain, emailAddress: String) {
    if (!mutableState.value.canManageDomains || mutableState.value.activeMutationId != null) return
    sendEmailCode(domain = domain, emailAddress = emailAddress, nextFlow = false)
  }

  fun selectEnrollmentMode(mode: OrganizationDomain.EnrollmentMode) {
    if (mode !in mutableState.value.enrollmentModeOptions) return
    mutableState.value =
      mutableState.value.copy(
        selectedEnrollmentMode = mode,
        deletePending =
          if (mode == OrganizationDomain.EnrollmentMode.ManualInvitation) {
            mutableState.value.deletePending
          } else {
            false
          },
      )
  }

  fun setDeletePending(deletePending: Boolean) {
    mutableState.value = mutableState.value.copy(deletePending = deletePending)
  }

  fun updateEnrollmentMode(domain: OrganizationDomain) {
    val current = mutableState.value
    if (!current.canUpdateEnrollmentMode) return

    mutableState.value =
      current.copy(activeMutationId = UPDATE_ENROLLMENT_MUTATION_ID, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      val deletePending =
        current.deletePending.takeIf {
          current.selectedEnrollmentMode == OrganizationDomain.EnrollmentMode.ManualInvitation
        }
      when (
        val result =
          domain.updateEnrollmentMode(
            enrollmentMode = current.selectedEnrollmentMode,
            deletePending = deletePending,
          )
      ) {
        is ClerkResult.Success -> {
          upsertDomain(result.value)
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              flow = OrganizationVerifiedDomainsFlow.DomainsList,
              deletePending = false,
            )
        }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  fun deleteDomain(domain: OrganizationDomain) {
    if (!mutableState.value.canManageDomains || mutableState.value.activeMutationId != null) return

    mutableState.value =
      mutableState.value.copy(activeMutationId = DELETE_DOMAIN_MUTATION_ID, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      when (val result = domain.delete()) {
        is ClerkResult.Success -> {
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              flow = OrganizationVerifiedDomainsFlow.DomainsList,
              domains = mutableState.value.domains.filterNot { it.id == domain.id },
              totalCount = (mutableState.value.totalCount - 1).coerceAtLeast(0),
            )
        }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  fun clearError() {
    mutableState.value = mutableState.value.copy(errorMessage = null)
  }

  private suspend fun loadDomains(reset: Boolean) {
    val currentOrganization = organization ?: return
    val current = mutableState.value
    if (!current.canLoadDomains) return

    mutableState.value = current.copy(isLoadingMore = !reset, errorMessage = null)
    val offset = if (reset) 0 else current.domains.size
    when (val result = currentOrganization.getDomains(limit = pageSize, offset = offset)) {
      is ClerkResult.Success -> applyDomainPage(page = result.value, append = !reset)
      is ClerkResult.Failure ->
        mutableState.value =
          mutableState.value.copy(isLoadingMore = false, errorMessage = result.errorMessage)
    }
  }

  private fun applyDomainPage(page: ClerkPaginatedResponse<OrganizationDomain>, append: Boolean) {
    val domains = if (append) mutableState.value.domains + page.data else page.data
    mutableState.value =
      mutableState.value.copy(
        domains = domains,
        totalCount = page.totalCount,
        hasNextPage = domains.size < page.totalCount,
        isLoadingMore = false,
      )
  }

  private fun sendEmailCode(domain: OrganizationDomain, emailAddress: String, nextFlow: Boolean) {
    mutableState.value =
      mutableState.value.copy(activeMutationId = SEND_CODE_MUTATION_ID, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      when (val result = domain.sendEmailCode(affiliationEmailAddress = emailAddress)) {
        is ClerkResult.Success -> {
          upsertDomain(result.value)
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              verificationCode = "",
              flow =
                if (nextFlow) {
                  OrganizationVerifiedDomainsFlow.VerifyCode(
                    domain = result.value,
                    emailAddress = emailAddress,
                  )
                } else {
                  mutableState.value.flow
                },
            )
        }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  private fun upsertDomain(domain: OrganizationDomain) {
    val current = mutableState.value
    val exists = current.domains.any { it.id == domain.id }
    val domains =
      if (exists) {
        current.domains.map { existing -> if (existing.id == domain.id) domain else existing }
      } else {
        listOf(domain) + current.domains
      }
    mutableState.value =
      current.copy(domains = domains, totalCount = current.totalCount + if (exists) 0 else 1)
  }

  private fun mutationFailed(errorMessage: String) {
    mutableState.value =
      mutableState.value.copy(activeMutationId = null, errorMessage = errorMessage)
  }
}

private fun String.sanitizedLocalPart(): String {
  return substringBefore("@").trim().take(AFFILIATION_EMAIL_LOCAL_PART_MAX_LENGTH)
}

private const val DEFAULT_PAGE_SIZE = 20
