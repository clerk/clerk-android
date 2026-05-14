package com.clerk.ui.organizationprofile.domains

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.userdata.PublicUserData
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationDomain
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationSystemPermission
import com.clerk.api.organizations.createDomain
import com.clerk.api.organizations.delete
import com.clerk.api.organizations.getDomains
import com.clerk.api.organizations.sendEmailCode
import com.clerk.api.organizations.updateEnrollmentMode
import com.clerk.api.organizations.verifyCode
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
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
import kotlinx.serialization.json.JsonNull

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizationVerifiedDomainsViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  @BeforeTest
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.organizationDomainsIsEnabled } returns true
    every { Clerk.organizationDomainEnrollmentModes } returns
      listOf("manual_invitation", "automatic_invitation", "automatic_suggestion")
    mockkStatic("com.clerk.api.organizations.OrganizationKt")
    mockkStatic("com.clerk.api.organizations.OrganizationDomainKt")
  }

  @AfterTest
  fun tearDown() {
    unmockkStatic("com.clerk.api.organizations.OrganizationKt")
    unmockkStatic("com.clerk.api.organizations.OrganizationDomainKt")
    unmockkAll()
  }

  @Test
  fun `load populates domains and enrollment options`() = runTest {
    val organization = organization()
    val first = domain(id = "dom_1", name = "example.com")
    coEvery { organization.getDomains(limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(first), totalCount = 1))

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
  fun `loadMoreDomains appends next page`() = runTest {
    val organization = organization()
    val first = domain(id = "dom_1", name = "example.com")
    val second = domain(id = "dom_2", name = "clerk.com")
    coEvery { organization.getDomains(limit = 1, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(first), totalCount = 2))
    coEvery { organization.getDomains(limit = 1, offset = 1) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(second), totalCount = 2))

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

    coVerify(exactly = 0) { organization.getDomains(limit = any(), offset = any()) }
    assertFalse(viewModel.state.value.canLoadDomains)
    assertEquals(emptyList(), viewModel.state.value.domains)
  }

  @Test
  fun `createDomain routes unverified domain to email verification`() = runTest {
    val organization = organization()
    val created = domain(id = "dom_1", verified = false)
    coEvery { organization.getDomains(limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))
    coEvery { organization.createDomain(name = "example.com") } returns ClerkResult.success(created)

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
    val codeSent = pending.copy(affiliationEmailAddress = "admin@example.com")
    val verified = codeSent.copy(verification = verification("verified"))
    coEvery { organization.getDomains(limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(pending), totalCount = 1))
    coEvery { pending.sendEmailCode(affiliationEmailAddress = "admin@example.com") } returns
      ClerkResult.success(codeSent)
    coEvery { codeSent.verifyCode(code = "123456") } returns ClerkResult.success(verified)

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
  fun `updateEnrollmentMode passes deletePending when switching to manual invitation`() = runTest {
    val organization = organization()
    val domain = domain(enrollmentMode = "automatic_suggestion")
    val updated = domain.copy(enrollmentMode = "manual_invitation")
    coEvery { organization.getDomains(limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(domain), totalCount = 1))
    coEvery {
      domain.updateEnrollmentMode(
        enrollmentMode = OrganizationDomain.EnrollmentMode.ManualInvitation,
        deletePending = true,
      )
    } returns ClerkResult.success(updated)

    val viewModel = viewModel(pageSize = 2)
    viewModel.load(organization = organization, membership = viewerMembership())
    advanceUntilIdle()
    viewModel.showEnrollmentMode(domain)
    viewModel.selectEnrollmentMode(OrganizationDomain.EnrollmentMode.ManualInvitation)
    viewModel.setDeletePending(true)
    viewModel.updateEnrollmentMode(domain)
    advanceUntilIdle()

    coVerify(exactly = 1) {
      domain.updateEnrollmentMode(
        enrollmentMode = OrganizationDomain.EnrollmentMode.ManualInvitation,
        deletePending = true,
      )
    }
    assertEquals(updated, viewModel.state.value.domains.single())
    assertEquals(OrganizationVerifiedDomainsFlow.DomainsList, viewModel.state.value.flow)
  }

  @Test
  fun `deleteDomain removes domain from state`() = runTest {
    val organization = organization()
    val domain = domain(id = "dom_1")
    coEvery { organization.getDomains(limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = listOf(domain), totalCount = 1))
    coEvery { domain.delete() } returns
      ClerkResult.success(
        DeletedObject(objectType = "organization_domain", id = domain.id, deleted = true)
      )

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
    coEvery { organization.getDomains(limit = 2, offset = 0) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))
    coEvery { organization.createDomain(name = "example.com") } returns
      ClerkResult.Failure(ClerkErrorResponse(errors = listOf(Error(longMessage = "boom"))))

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
    return OrganizationVerifiedDomainsViewModel(pageSize = pageSize, dispatcher = dispatcher)
  }

  private fun organization(): Organization {
    return Organization(
      id = "org_123",
      name = "Acme",
      slug = "acme",
      imageUrl = "",
      maxAllowedMemberships = 0,
      adminDeleteEnabled = true,
      createdAt = 1,
      updatedAt = 1,
      publicMetadata = JsonNull,
    )
  }

  private fun viewerMembership(
    permissions: List<OrganizationSystemPermission> =
      listOf(OrganizationSystemPermission.READ_DOMAINS, OrganizationSystemPermission.MANAGE_DOMAINS)
  ): OrganizationMembership {
    return OrganizationMembership(
      id = "mem_viewer",
      publicMetadata = JsonNull,
      role = "org:admin",
      roleName = "Admin",
      permissions = permissions.map { it.value },
      publicUserData =
        PublicUserData(
          firstName = "Viewer",
          lastName = null,
          imageUrl = "",
          hasImage = false,
          identifier = "viewer@example.com",
          userId = "user_viewer",
        ),
      organization = organization(),
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun domain(
    id: String = "dom_1",
    name: String = "example.com",
    enrollmentMode: String = "manual_invitation",
    verified: Boolean = true,
  ): OrganizationDomain {
    return OrganizationDomain(
      id = id,
      name = name,
      organizationId = "org_123",
      enrollmentMode = enrollmentMode,
      verification = verification(status = if (verified) "verified" else "unverified"),
      affiliationEmailAddress = null,
      totalPendingInvitations = 0,
      totalPendingSuggestions = 0,
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun verification(status: String): OrganizationDomain.Verification {
    return OrganizationDomain.Verification(
      status = status,
      strategy = "email_code",
      attempts = 0,
      expireAt = null,
    )
  }
}
