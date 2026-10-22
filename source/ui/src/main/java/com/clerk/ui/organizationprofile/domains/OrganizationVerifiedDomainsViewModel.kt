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
import kotlinx.coroutines.flow.update
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
  private var lastLoadRequest: DomainsLoadRequest? = null

  fun load(
    organization: Organization,
    membership: OrganizationMembership?,
    domainsEnabled: Boolean = Clerk.organizationDomainsIsEnabled,
    enrollmentModes: List<String> = Clerk.organizationDomainEnrollmentModes,
  ) {
    val request =
      DomainsLoadRequest(organizationId = organization.id, membershipId = membership?.id)
    if (request == lastLoadRequest) return
    lastLoadRequest = request
    reload(organization, membership, domainsEnabled, enrollmentModes)
  }

  fun retry() {
    val currentOrganization = organization ?: return
    reload(
      organization = currentOrganization,
      membership = membership,
      domainsEnabled = domainsEnabled,
      enrollmentModes = Clerk.organizationDomainEnrollmentModes,
    )
  }

  private fun reload(
    organization: Organization,
    membership: OrganizationMembership?,
    domainsEnabled: Boolean,
    enrollmentModes: List<String>,
  ) {
    this.organization = organization
    this.membership = membership
    this.domainsEnabled = domainsEnabled

    val canReadDomains = membership?.canReadDomains == true
    val canManageDomains = membership?.canManageDomains == true
    val options = enrollmentModeOptions(enrollmentModes)
    val shouldLoad = domainsEnabled && (canReadDomains || canManageDomains)

    mutableState.update { current ->
      val permitted =
        current.copy(
          domainsEnabled = domainsEnabled,
          canReadDomains = canReadDomains,
          canManageDomains = canManageDomains,
          enrollmentModeOptions = options,
          selectedEnrollmentMode =
            current.selectedEnrollmentMode.takeIf { it in options } ?: options.first(),
          isLoadingInitial = shouldLoad,
          errorMessage = null,
        )
      if (shouldLoad) {
        permitted
      } else {
        permitted.copy(domains = emptyList(), totalCount = 0, hasNextPage = false)
      }
    }
    if (!shouldLoad) return

    viewModelScope.launch(dispatcher) {
      loadDomains(reset = true)
      mutableState.update { it.copy(isLoadingInitial = false) }
    }
  }

  fun loadMoreDomains() {
    val current = mutableState.value
    if (!current.hasNextPage || current.isLoadingMore) return
    viewModelScope.launch(dispatcher) { loadDomains(reset = false) }
  }

  fun showAddDomain() {
    mutableState.update { current ->
      if (!current.canManageDomains) return@update current
      current.copy(
        flow = OrganizationVerifiedDomainsFlow.AddDomain,
        domainName = "",
        errorMessage = null,
      )
    }
  }

  fun showVerifyEmail(domain: OrganizationDomain) {
    mutableState.update { current ->
      if (!current.canManageDomains) return@update current
      current.copy(
        flow = OrganizationVerifiedDomainsFlow.VerifyEmail(domain),
        affiliationEmailLocalPart = domain.affiliationEmailAddress?.substringBefore("@").orEmpty(),
        verificationCode = "",
        errorMessage = null,
      )
    }
  }

  fun showEnrollmentMode(domain: OrganizationDomain) {
    mutableState.update { current ->
      if (!current.canManageDomains || !domain.isVerified) return@update current
      val selected =
        domain.enrollmentModeType.takeIf {
          it !is OrganizationDomain.EnrollmentMode.Unknown && it in current.enrollmentModeOptions
        } ?: current.enrollmentModeOptions.first()
      current.copy(
        flow = OrganizationVerifiedDomainsFlow.EnrollmentMode(domain),
        selectedEnrollmentMode = selected,
        errorMessage = null,
      )
    }
  }

  fun showDeleteDomain(domain: OrganizationDomain) {
    mutableState.update { current ->
      if (!current.canManageDomains) return@update current
      current.copy(flow = OrganizationVerifiedDomainsFlow.DeleteDomain(domain), errorMessage = null)
    }
  }

  fun dismissFlow() {
    mutableState.update {
      it.copy(
        flow = OrganizationVerifiedDomainsFlow.DomainsList,
        domainName = "",
        affiliationEmailLocalPart = "",
        verificationCode = "",
        activeMutationId = null,
        errorMessage = null,
      )
    }
  }

  fun setDomainName(domainName: String) {
    mutableState.update { it.copy(domainName = domainName) }
  }

  fun createDomain() {
    val currentOrganization = organization ?: return
    val current = mutableState.value
    val name = current.domainName.trim()
    if (!current.canCreateDomain || name.isEmpty()) return

    mutableState.update {
      it.copy(activeMutationId = CREATE_DOMAIN_MUTATION_ID, errorMessage = null)
    }
    viewModelScope.launch(dispatcher) {
      when (val result = currentOrganization.createDomain(name = name)) {
        is ClerkResult.Success ->
          mutableState.update {
            it
              .withUpsertedDomain(result.value)
              .copy(
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
    mutableState.update { it.copy(affiliationEmailLocalPart = localPart.sanitizedLocalPart()) }
  }

  fun sendAffiliationEmail(domain: OrganizationDomain) {
    val current = mutableState.value
    val localPart = current.affiliationEmailLocalPart.trim()
    if (!current.canSendVerificationEmail || localPart.isEmpty()) return
    val emailAddress = "$localPart@${domain.name}"

    sendEmailCode(domain = domain, emailAddress = emailAddress, nextFlow = true)
  }

  fun setVerificationCode(code: String) {
    mutableState.update { it.copy(verificationCode = code) }
  }

  fun verifyCode(domain: OrganizationDomain) {
    val current = mutableState.value
    val code = current.verificationCode.trim()
    if (!current.canVerifyCode || code.isEmpty()) return

    mutableState.update { it.copy(activeMutationId = VERIFY_CODE_MUTATION_ID, errorMessage = null) }
    viewModelScope.launch(dispatcher) {
      when (val result = domain.verifyCode(code = code)) {
        is ClerkResult.Success ->
          mutableState.update {
            it
              .withUpsertedDomain(result.value)
              .copy(
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
    mutableState.update { current ->
      if (mode in current.enrollmentModeOptions) current.copy(selectedEnrollmentMode = mode)
      else current
    }
  }

  fun updateEnrollmentMode(domain: OrganizationDomain) {
    val current = mutableState.value
    if (!current.canUpdateEnrollmentMode) return

    mutableState.update {
      it.copy(activeMutationId = UPDATE_ENROLLMENT_MUTATION_ID, errorMessage = null)
    }
    viewModelScope.launch(dispatcher) {
      when (
        val result = domain.updateEnrollmentMode(enrollmentMode = current.selectedEnrollmentMode)
      ) {
        is ClerkResult.Success ->
          mutableState.update {
            it
              .withUpsertedDomain(result.value)
              .copy(activeMutationId = null, flow = OrganizationVerifiedDomainsFlow.DomainsList)
          }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  fun deleteDomain(domain: OrganizationDomain) {
    if (!mutableState.value.canManageDomains || mutableState.value.activeMutationId != null) return

    mutableState.update {
      it.copy(activeMutationId = DELETE_DOMAIN_MUTATION_ID, errorMessage = null)
    }
    viewModelScope.launch(dispatcher) {
      when (val result = domain.delete()) {
        is ClerkResult.Success ->
          mutableState.update { current ->
            current.copy(
              activeMutationId = null,
              flow = OrganizationVerifiedDomainsFlow.DomainsList,
              domains = current.domains.filterNot { it.id == domain.id },
              totalCount = (current.totalCount - 1).coerceAtLeast(0),
            )
          }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  fun clearError() {
    mutableState.update { it.copy(errorMessage = null) }
  }

  private suspend fun loadDomains(reset: Boolean) {
    val currentOrganization = organization ?: return
    val current = mutableState.value
    if (!current.canLoadDomains) return

    mutableState.update { it.copy(isLoadingMore = !reset, errorMessage = null) }
    val offset = if (reset) 0 else current.domains.size
    when (val result = currentOrganization.getDomains(limit = pageSize, offset = offset)) {
      is ClerkResult.Success -> applyDomainPage(page = result.value, append = !reset)
      is ClerkResult.Failure ->
        mutableState.update { it.copy(isLoadingMore = false, errorMessage = result.errorMessage) }
    }
  }

  private fun applyDomainPage(page: ClerkPaginatedResponse<OrganizationDomain>, append: Boolean) {
    mutableState.update { current ->
      val domains = if (append) current.domains + page.data else page.data
      current.copy(
        domains = domains,
        totalCount = page.totalCount,
        hasNextPage = domains.size < page.totalCount,
        isLoadingMore = false,
      )
    }
  }

  private fun sendEmailCode(domain: OrganizationDomain, emailAddress: String, nextFlow: Boolean) {
    mutableState.update { it.copy(activeMutationId = SEND_CODE_MUTATION_ID, errorMessage = null) }
    viewModelScope.launch(dispatcher) {
      when (val result = domain.sendEmailCode(affiliationEmailAddress = emailAddress)) {
        is ClerkResult.Success ->
          mutableState.update { current ->
            current
              .withUpsertedDomain(result.value)
              .copy(
                activeMutationId = null,
                verificationCode = "",
                flow =
                  if (nextFlow) {
                    OrganizationVerifiedDomainsFlow.VerifyCode(
                      domain = result.value,
                      emailAddress = emailAddress,
                    )
                  } else {
                    current.flow
                  },
              )
          }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  private fun mutationFailed(errorMessage: String) {
    mutableState.update { it.copy(activeMutationId = null, errorMessage = errorMessage) }
  }

  private data class DomainsLoadRequest(val organizationId: String, val membershipId: String?)
}

private fun OrganizationVerifiedDomainsState.withUpsertedDomain(
  domain: OrganizationDomain
): OrganizationVerifiedDomainsState {
  val exists = domains.any { it.id == domain.id }
  val updatedDomains =
    if (exists) {
      domains.map { existing -> if (existing.id == domain.id) domain else existing }
    } else {
      listOf(domain) + domains
    }
  return copy(domains = updatedDomains, totalCount = totalCount + if (exists) 0 else 1)
}

private fun String.sanitizedLocalPart(): String {
  return substringBefore("@").trim().take(AFFILIATION_EMAIL_LOCAL_PART_MAX_LENGTH)
}

private const val DEFAULT_PAGE_SIZE = 20
