@file:Suppress("TooManyFunctions")

package com.clerk.ui.organizationprofile.domains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import com.clerk.ui.organizationprofile.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OrganizationVerifiedDomainsViewModel(
  private val clerk: Clerk,
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
    domainsEnabled: Boolean = clerk.environment.organizationSettings.domains.enabled,
    enrollmentModes: List<OrganizationEnrollmentMode> =
      clerk.environment.organizationSettings.domains.enrollmentModes,
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
      enrollmentModes = clerk.environment.organizationSettings.domains.enrollmentModes,
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
      domain.enrollmentMode.takeIf {
        it !is OrganizationEnrollmentMode.Unrecognized && it in current.enrollmentModeOptions
      } ?: current.enrollmentModeOptions.first()
    mutableState.value =
      current.copy(
        flow = OrganizationVerifiedDomainsFlow.EnrollmentMode(domain),
        selectedEnrollmentMode = selected,
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
      runUiOperation { currentOrganization.createDomain(name) }
        .onSuccess { result ->
          upsertDomain(result)
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              domainName = "",
              affiliationEmailLocalPart =
                result.affiliationEmailAddress?.substringBefore("@").orEmpty(),
              flow =
                if (result.isVerified) OrganizationVerifiedDomainsFlow.DomainsList
                else OrganizationVerifiedDomainsFlow.VerifyEmail(result),
            )
        }
        .onFailure { failure ->
          mutationFailed(failure.displayMessage)
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
      runUiOperation {
          domain.attemptAffiliationVerification(AttemptAffiliationVerificationParams(code))
        }
        .onSuccess { result ->
          upsertDomain(result)
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              verificationCode = "",
              flow = OrganizationVerifiedDomainsFlow.DomainsList,
            )
        }
        .onFailure { failure ->
          mutationFailed(failure.displayMessage)
        }
    }
  }

  fun resendVerificationCode(domain: OrganizationDomain, emailAddress: String) {
    if (!mutableState.value.canManageDomains || mutableState.value.activeMutationId != null) return
    sendEmailCode(domain = domain, emailAddress = emailAddress, nextFlow = false)
  }

  fun selectEnrollmentMode(mode: OrganizationEnrollmentMode) {
    if (mode !in mutableState.value.enrollmentModeOptions) return
    mutableState.value = mutableState.value.copy(selectedEnrollmentMode = mode)
  }

  fun updateEnrollmentMode(domain: OrganizationDomain) {
    val current = mutableState.value
    if (!current.canUpdateEnrollmentMode) return

    mutableState.value =
      current.copy(activeMutationId = UPDATE_ENROLLMENT_MUTATION_ID, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      runUiOperation {
          domain.updateEnrollmentMode(UpdateEnrollmentModeParams(current.selectedEnrollmentMode))
        }
        .onSuccess { result ->
          upsertDomain(result)
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              flow = OrganizationVerifiedDomainsFlow.DomainsList,
            )
        }
        .onFailure { failure ->
          mutationFailed(failure.displayMessage)
        }
    }
  }

  fun deleteDomain(domain: OrganizationDomain) {
    if (!mutableState.value.canManageDomains || mutableState.value.activeMutationId != null) return

    mutableState.value =
      mutableState.value.copy(activeMutationId = DELETE_DOMAIN_MUTATION_ID, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      runUiOperation { domain.delete() }
        .onSuccess { result ->
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              flow = OrganizationVerifiedDomainsFlow.DomainsList,
              domains = mutableState.value.domains.filterNot { it.id == domain.id },
              totalCount = (mutableState.value.totalCount - 1).coerceAtLeast(0),
            )
        }
        .onFailure { failure ->
          mutationFailed(failure.displayMessage)
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
    runUiOperation {
        currentOrganization.getDomains(
          GetDomainsParams(
            initialPage = offset.toDouble() / pageSize + 1,
            pageSize = pageSize.toDouble(),
          )
        )
      }
      .onSuccess { result ->
        applyDomainPage(page = result, append = !reset)
      }
      .onFailure { failure ->
        mutableState.value =
          mutableState.value.copy(isLoadingMore = false, errorMessage = failure.displayMessage)
      }
  }

  private fun applyDomainPage(page: ClerkPaginatedResponseOrganizationDomain, append: Boolean) {
    val domains = if (append) mutableState.value.domains + page.data else page.data
    mutableState.value =
      mutableState.value.copy(
        domains = domains,
        totalCount = page.totalCount.toInt(),
        hasNextPage = domains.size < page.totalCount,
        isLoadingMore = false,
      )
  }

  private fun sendEmailCode(domain: OrganizationDomain, emailAddress: String, nextFlow: Boolean) {
    mutableState.value =
      mutableState.value.copy(activeMutationId = SEND_CODE_MUTATION_ID, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      runUiOperation {
          domain.prepareAffiliationVerification(PrepareAffiliationVerificationParams(emailAddress))
        }
        .onSuccess { result ->
          upsertDomain(result)
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              verificationCode = "",
              flow =
                if (nextFlow) {
                  OrganizationVerifiedDomainsFlow.VerifyCode(
                    domain = result,
                    emailAddress = emailAddress,
                  )
                } else {
                  mutableState.value.flow
                },
            )
        }
        .onFailure { failure ->
          mutationFailed(failure.displayMessage)
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
