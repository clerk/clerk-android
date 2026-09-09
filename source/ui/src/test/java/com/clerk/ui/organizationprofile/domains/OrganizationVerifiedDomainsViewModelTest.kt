package com.clerk.ui.organizationprofile.domains

import com.clerk.api.*
import com.clerk.testing.mockClerk
import com.clerk.testing.testCoreError
import com.clerk.ui.organizationprofile.isVerified
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.unmockkAll
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationVerifiedDomainsViewModelTest {
  private val clerk = mockClerk()

  private val dispatcher = StandardTestDispatcher()
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  @BeforeTest
  fun setUp() {
    every { clerk.environment.organizationSettings.domains.enabled } returns true
    every { clerk.environment.organizationSettings.domains.enrollmentModes } returns
      defaultEnrollmentModeOptions
  }

  @AfterTest
  fun tearDown() {

    unmockkAll()
  }

  @Test
  fun `load populates domains and enrollment options`() = runTest {
    val organization = organization()
    val first = domain(id = "dom_1", name = "example.com")
    coEvery { organization.getDomains(GetDomainsParams(pageSize = 2.0, initialPage = 1.0)) } returns
      ClerkPaginatedResponseOrganizationDomain(data = listOf(first), totalCount = 1.0)

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewerMembership())
    advanceUntilIdle()

    val state = viewModel.state.value
    assertEquals(listOf(first), state.domains)
    assertEquals(1, state.totalCount)
    assertFalse(state.hasNextPage)
    assertFalse(state.isLoadingInitial)
    assertTrue(state.canReadDomains)
    assertTrue(state.canManageDomains)
    assertEquals(defaultEnrollmentModeOptions, state.enrollmentModeOptions)
  }

  @Test
  fun `enrollment options include automatic invitations when client config omits it`() {
    val options =
      enrollmentModeOptions(
        listOf(
          OrganizationEnrollmentMode.ManualInvitation,
          OrganizationEnrollmentMode.AutomaticSuggestion,
        )
      )

    assertEquals(defaultEnrollmentModeOptions, options)
  }

  @Test
  fun `loadMoreDomains appends next page`() = runTest {
    val organization = organization()
    val first = domain(id = "dom_1", name = "example.com")
    val second = domain(id = "dom_2", name = "clerk.com")
    coEvery { organization.getDomains(GetDomainsParams(pageSize = 1.0, initialPage = 1.0)) } returns
      ClerkPaginatedResponseOrganizationDomain(data = listOf(first), totalCount = 2.0)
    coEvery { organization.getDomains(GetDomainsParams(pageSize = 1.0, initialPage = 2.0)) } returns
      ClerkPaginatedResponseOrganizationDomain(data = listOf(second), totalCount = 2.0)

    val viewModel = viewModel(pageSize = 1)
    viewModel.load(organization = organization, membership = viewerMembership())
    advanceUntilIdle()
    viewModel.loadMoreDomains()
    advanceUntilIdle()

    assertEquals(listOf(first, second), viewModel.state.value.domains)
    assertFalse(viewModel.state.value.hasNextPage)
  }

  @Test
  fun `load skips network when domains are disabled or unavailable to viewer`() = runTest {
    val organization = organization()
    val viewModel = viewModel(pageSize = 2)

    viewModel.load(
      organization = organization,
      membership = viewerMembership(permissions = emptyList()),
      domainsEnabled = true,
    )
    advanceUntilIdle()

    coVerify(exactly = 0) { organization.getDomains(any()) }
    assertFalse(viewModel.state.value.canLoadDomains)
    assertEquals(emptyList(), viewModel.state.value.domains)
  }

  @Test
  fun `createDomain routes unverified domain to email verification`() = runTest {
    val organization = organization()
    val created = domain(id = "dom_1", verified = false)
    coEvery { organization.getDomains(GetDomainsParams(pageSize = 2.0, initialPage = 1.0)) } returns
      ClerkPaginatedResponseOrganizationDomain(data = emptyList(), totalCount = 0.0)
    coEvery { organization.createDomain(domainName = "example.com") } returns created

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewerMembership())
    advanceUntilIdle()
    viewModel.showAddDomain()
    viewModel.setDomainName("example.com")
    viewModel.createDomain()
    advanceUntilIdle()

    val flow = assertIs<OrganizationVerifiedDomainsFlow.VerifyEmail>(viewModel.state.value.flow)
    assertEquals(created, flow.domain)
    assertEquals(listOf(created), viewModel.state.value.domains)
    assertEquals(1, viewModel.state.value.totalCount)
    assertNull(viewModel.state.value.errorMessage)
  }

  @Test
  fun `send affiliation email then verify code updates domain`() = runTest {
    val organization = organization()
    val pending = domain(id = "dom_1", verified = false)
    val codeSent = domain(verified = false, affiliationEmailAddress = "admin@example.com")
    val verified = domain(verified = true, affiliationEmailAddress = "admin@example.com")
    coEvery { organization.getDomains(GetDomainsParams(pageSize = 2.0, initialPage = 1.0)) } returns
      ClerkPaginatedResponseOrganizationDomain(data = listOf(pending), totalCount = 1.0)
    coEvery {
      pending.prepareAffiliationVerification(
        PrepareAffiliationVerificationParams("admin@example.com")
      )
    } returns codeSent
    coEvery {
      codeSent.attemptAffiliationVerification(AttemptAffiliationVerificationParams("123456"))
    } returns verified

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewerMembership())
    advanceUntilIdle()
    viewModel.showVerifyEmail(pending)
    viewModel.setAffiliationEmailLocalPart("admin@example.com")
    viewModel.sendAffiliationEmail(pending)
    advanceUntilIdle()

    val verifyFlow =
      assertIs<OrganizationVerifiedDomainsFlow.VerifyCode>(viewModel.state.value.flow)
    assertEquals("admin@example.com", verifyFlow.emailAddress)
    assertEquals(codeSent, verifyFlow.domain)

    viewModel.setVerificationCode("123456")
    viewModel.verifyCode(codeSent)
    advanceUntilIdle()

    assertEquals(OrganizationVerifiedDomainsFlow.DomainsList, viewModel.state.value.flow)
    assertTrue(viewModel.state.value.domains.single().isVerified)
  }

  @Test
  fun `updateEnrollmentMode saves selected enrollment mode`() = runTest {
    val organization = organization()
    val domain = domain(enrollmentMode = "automatic_suggestion")
    val updated = domain(enrollmentMode = "manual_invitation")
    coEvery { organization.getDomains(GetDomainsParams(pageSize = 2.0, initialPage = 1.0)) } returns
      ClerkPaginatedResponseOrganizationDomain(data = listOf(domain), totalCount = 1.0)
    coEvery {
      domain.updateEnrollmentMode(
        UpdateEnrollmentModeParams(OrganizationEnrollmentMode.ManualInvitation)
      )
    } returns updated

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewerMembership())
    advanceUntilIdle()
    viewModel.showEnrollmentMode(domain)
    viewModel.selectEnrollmentMode(OrganizationEnrollmentMode.ManualInvitation)
    viewModel.updateEnrollmentMode(domain)
    advanceUntilIdle()

    coVerify(exactly = 1) {
      domain.updateEnrollmentMode(
        UpdateEnrollmentModeParams(OrganizationEnrollmentMode.ManualInvitation)
      )
    }
    assertEquals(updated, viewModel.state.value.domains.single())
    assertEquals(OrganizationVerifiedDomainsFlow.DomainsList, viewModel.state.value.flow)
  }

  @Test
  fun `deleteDomain removes domain from state`() = runTest {
    val organization = organization()
    val domain = domain(id = "dom_1")
    coEvery { organization.getDomains(GetDomainsParams(pageSize = 2.0, initialPage = 1.0)) } returns
      ClerkPaginatedResponseOrganizationDomain(data = listOf(domain), totalCount = 1.0)
    coEvery { domain.delete() } returns mockk(relaxed = true)

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewerMembership())
    advanceUntilIdle()
    viewModel.showDeleteDomain(domain)
    viewModel.deleteDomain(domain)
    advanceUntilIdle()

    assertEquals(emptyList(), viewModel.state.value.domains)
    assertEquals(0, viewModel.state.value.totalCount)
    assertEquals(OrganizationVerifiedDomainsFlow.DomainsList, viewModel.state.value.flow)
  }

  @Test
  fun `mutation failure keeps current flow and stores error`() = runTest {
    val organization = organization()
    coEvery { organization.getDomains(GetDomainsParams(pageSize = 2.0, initialPage = 1.0)) } returns
      ClerkPaginatedResponseOrganizationDomain(data = emptyList(), totalCount = 0.0)
    coEvery { organization.createDomain(domainName = "example.com") } throws testCoreError("boom")

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewerMembership())
    advanceUntilIdle()
    viewModel.showAddDomain()
    viewModel.setDomainName("example.com")
    viewModel.createDomain()
    advanceUntilIdle()

    assertEquals(OrganizationVerifiedDomainsFlow.AddDomain, viewModel.state.value.flow)
    assertEquals("boom", viewModel.state.value.errorMessage)
    assertNull(viewModel.state.value.activeMutationId)
  }

  @Test
  fun `affiliation email local part is capped and strips pasted domains`() = runTest {
    val viewModel = viewModel(pageSize = 2)

    viewModel.setAffiliationEmailLocalPart("averylongverificationaddress@example.com")

    assertEquals(
      "averylongverificationaddress".take(AFFILIATION_EMAIL_LOCAL_PART_MAX_LENGTH),
      viewModel.state.value.affiliationEmailLocalPart,
    )
  }

  private fun viewModel(pageSize: Int): OrganizationVerifiedDomainsViewModel {
    return OrganizationVerifiedDomainsViewModel(clerk, pageSize = pageSize, dispatcher = dispatcher)
  }

  private fun organization(): Organization =
    mockk(relaxed = true) { every { id } returns "org_123" }

  private fun viewerMembership(
    permissions: List<String> = listOf("org:sys_domains:read", "org:sys_domains:manage")
  ): OrganizationMembership {
    val membership = mockk<OrganizationMembership>(relaxed = true)
    every { membership.permissions } returns permissions
    return membership
  }

  private fun domain(
    id: String = "dom_1",
    name: String = "example.com",
    enrollmentMode: String = "manual_invitation",
    verified: Boolean = true,
    affiliationEmailAddress: String? = null,
  ): OrganizationDomain {
    val domain = mockk<OrganizationDomain>(relaxed = true)
    every { domain.id } returns id
    every { domain.name } returns name
    every { domain.enrollmentMode } returns
      when (enrollmentMode) {
        "automatic_suggestion" -> OrganizationEnrollmentMode.AutomaticSuggestion
        "automatic_invitation" -> OrganizationEnrollmentMode.AutomaticInvitation
        else -> OrganizationEnrollmentMode.ManualInvitation
      }
    every { domain.affiliationEmailAddress } returns affiliationEmailAddress
    every { domain.verification } returns
      OrganizationDomainVerification(
        if (verified) OrganizationDomainVerificationStatus.Verified
        else OrganizationDomainVerificationStatus.Unverified,
        0.0,
        Instant.parse("2030-01-01T00:00:00Z"),
      )
    return domain
  }
}
