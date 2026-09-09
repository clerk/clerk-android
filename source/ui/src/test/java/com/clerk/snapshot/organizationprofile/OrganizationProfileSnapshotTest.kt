package com.clerk.snapshot.organizationprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.*
import com.clerk.base.BaseSnapshotTest
import com.clerk.testing.*
import com.clerk.ui.R
import com.clerk.ui.organizationprofile.actions.OrganizationProfileActionConfirmationActions
import com.clerk.ui.organizationprofile.actions.OrganizationProfileActionConfirmationContent
import com.clerk.ui.organizationprofile.actions.OrganizationProfileActionConfirmationState
import com.clerk.ui.organizationprofile.actions.OrganizationProfileConfirmationAction
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRowPlacement
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRowIcon
import com.clerk.ui.organizationprofile.domains.OrganizationVerifiedDomainsActions
import com.clerk.ui.organizationprofile.domains.OrganizationVerifiedDomainsContent
import com.clerk.ui.organizationprofile.domains.OrganizationVerifiedDomainsFlow
import com.clerk.ui.organizationprofile.domains.OrganizationVerifiedDomainsState
import com.clerk.ui.organizationprofile.members.OrganizationMembersActions
import com.clerk.ui.organizationprofile.members.OrganizationMembersContent
import com.clerk.ui.organizationprofile.members.OrganizationMembersState
import com.clerk.ui.organizationprofile.members.OrganizationMembersTab
import com.clerk.ui.organizationprofile.root.OrganizationProfileRootView
import com.clerk.ui.theme.ClerkMaterialTheme
import io.mockk.*
import java.time.Instant
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class OrganizationProfileSnapshotTest : BaseSnapshotTest() {

  @Test
  fun organizationProfileRoot() {
    snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.size(740.dp).background(ClerkMaterialTheme.colors.background)) {
          OrganizationProfileRootView(
            modifier = Modifier.fillMaxSize(),
            organization = snapshotOrganization(),
            membership = snapshotMembership(),
            onBackPressed = {},
            onUpdateProfile = {},
            onAction = {},
          )
        }
      }
    }
  }

  @Test
  fun organizationProfileRootWithCustomRows() {
    snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.size(740.dp).background(ClerkMaterialTheme.colors.background)) {
          OrganizationProfileRootView(
            modifier = Modifier.fillMaxSize(),
            organization = snapshotOrganization(),
            membership = snapshotMembership(),
            onBackPressed = {},
            onUpdateProfile = {},
            onAction = {},
            customRows =
              persistentListOf(
                OrganizationProfileCustomRow(
                  routeKey = "billing",
                  title = "Billing",
                  icon = OrganizationProfileRowIcon.Resource(R.drawable.ic_credit_card),
                  placement =
                    OrganizationProfileCustomRowPlacement.After(OrganizationProfileRow.Members),
                ),
                OrganizationProfileCustomRow(
                  routeKey = "preferences",
                  title = "Preferences",
                  icon = OrganizationProfileRowIcon.Resource(R.drawable.ic_cog),
                  placement =
                    OrganizationProfileCustomRowPlacement.Before(
                      OrganizationProfileRow.LeaveOrganization
                    ),
                ),
              ),
          )
        }
      }
    }
  }

  @Test
  fun organizationMembersLoading() {
    snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          viewerMembership = snapshotMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Members,
              isLoadingInitial = true,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersMembersTab() {
    snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          viewerMembership = snapshotMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Members,
              roles = sampleRoles,
              members = listOf(sampleMember("mem_1", "Ada", "Lovelace")),
              membersTotalCount = 1,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersInvitationsTab() {
    snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          viewerMembership = snapshotMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Invitations,
              roles = sampleRoles,
              invitations = listOf(sampleInvitation("inv_1")),
              invitationsTotalCount = 1,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersInvitationsEmptyState() {
    snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          viewerMembership = snapshotMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Invitations,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersRequestsTab() {
    snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          viewerMembership = snapshotMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Requests,
              requests = listOf(sampleRequest("req_1")),
              requestsTotalCount = 1,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersRequestsEmptyState() {
    snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          viewerMembership = snapshotMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Requests,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersEmptyState() {
    snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          viewerMembership = snapshotMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Members,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersSearchEmptyState() {
    snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          viewerMembership = snapshotMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Members,
              memberQuery = "ada",
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersSearchLoading() {
    snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          viewerMembership = snapshotMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Members,
              memberQuery = "ada",
              isSearchingMembers = true,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationVerifiedDomainsLoading() {
    snapshot {
      DomainsSnapshotSurface {
        OrganizationVerifiedDomainsContent(
          state =
            OrganizationVerifiedDomainsState(
              domainsEnabled = true,
              canReadDomains = true,
              canManageDomains = true,
              isLoadingInitial = true,
            ),
          actions = noOpDomainActions,
        )
      }
    }
  }

  @Test
  fun organizationVerifiedDomainsList() {
    snapshot {
      DomainsSnapshotSurface {
        OrganizationVerifiedDomainsContent(
          state =
            OrganizationVerifiedDomainsState(
              domainsEnabled = true,
              canReadDomains = true,
              canManageDomains = true,
              domains =
                listOf(
                  sampleDomain("dom_1", "example.com"),
                  sampleDomain(
                    "dom_2",
                    "pending.example.com",
                    verified = false,
                    enrollmentMode = "automatic_invitation",
                  ),
                ),
              totalCount = 2,
            ),
          actions = noOpDomainActions,
        )
      }
    }
  }

  @Test
  fun organizationVerifiedDomainsEmptyState() {
    snapshot {
      DomainsSnapshotSurface {
        OrganizationVerifiedDomainsContent(
          state =
            OrganizationVerifiedDomainsState(
              domainsEnabled = true,
              canReadDomains = true,
              canManageDomains = false,
            ),
          actions = noOpDomainActions,
        )
      }
    }
  }

  @Test
  fun organizationVerifiedDomainsAddDomain() {
    snapshot {
      DomainsSnapshotSurface {
        OrganizationVerifiedDomainsContent(
          state =
            OrganizationVerifiedDomainsState(
              domainsEnabled = true,
              canReadDomains = true,
              canManageDomains = true,
              flow = OrganizationVerifiedDomainsFlow.AddDomain,
              domainName = "example.com",
            ),
          actions = noOpDomainActions,
        )
      }
    }
  }

  @Test
  fun organizationVerifiedDomainsVerifyEmail() {
    val domain = sampleDomain("dom_1", "example.com", verified = false)
    snapshot {
      DomainsSnapshotSurface {
        OrganizationVerifiedDomainsContent(
          state =
            OrganizationVerifiedDomainsState(
              domainsEnabled = true,
              canReadDomains = true,
              canManageDomains = true,
              flow = OrganizationVerifiedDomainsFlow.VerifyEmail(domain),
              affiliationEmailLocalPart = "admin",
            ),
          actions = noOpDomainActions,
        )
      }
    }
  }

  @Test
  fun organizationVerifiedDomainsVerifyCode() {
    val domain = sampleDomain("dom_1", "example.com", verified = false)
    snapshot {
      DomainsSnapshotSurface {
        OrganizationVerifiedDomainsContent(
          state =
            OrganizationVerifiedDomainsState(
              domainsEnabled = true,
              canReadDomains = true,
              canManageDomains = true,
              flow =
                OrganizationVerifiedDomainsFlow.VerifyCode(
                  domain = domain,
                  emailAddress = "admin@example.com",
                ),
              verificationCode = "123456",
            ),
          actions = noOpDomainActions,
        )
      }
    }
  }

  @Test
  fun organizationVerifiedDomainsEnrollmentMode() {
    val domain = sampleDomain("dom_1", "example.com", enrollmentMode = "automatic_suggestion")
    snapshot {
      DomainsSnapshotSurface {
        OrganizationVerifiedDomainsContent(
          state =
            OrganizationVerifiedDomainsState(
              domainsEnabled = true,
              canReadDomains = true,
              canManageDomains = true,
              flow = OrganizationVerifiedDomainsFlow.EnrollmentMode(domain),
              selectedEnrollmentMode = OrganizationEnrollmentMode.ManualInvitation,
            ),
          actions = noOpDomainActions,
        )
      }
    }
  }

  @Test
  fun organizationVerifiedDomainsDeleteConfirmation() {
    val domain = sampleDomain("dom_1", "example.com")
    snapshot {
      DomainsSnapshotSurface {
        OrganizationVerifiedDomainsContent(
          state =
            OrganizationVerifiedDomainsState(
              domainsEnabled = true,
              canReadDomains = true,
              canManageDomains = true,
              flow = OrganizationVerifiedDomainsFlow.DeleteDomain(domain),
            ),
          actions = noOpDomainActions,
        )
      }
    }
  }

  @Test
  fun organizationVerifiedDomainsErrorState() {
    snapshot {
      DomainsSnapshotSurface {
        OrganizationVerifiedDomainsContent(
          state =
            OrganizationVerifiedDomainsState(
              domainsEnabled = true,
              canReadDomains = true,
              canManageDomains = true,
              flow = OrganizationVerifiedDomainsFlow.AddDomain,
              domainName = "example.com",
              errorMessage = "Unable to add domain",
            ),
          actions = noOpDomainActions,
        )
      }
    }
  }

  @Test
  fun organizationActionLeaveDisabled() {
    snapshot {
      ActionSnapshotSurface {
        OrganizationProfileActionConfirmationContent(
          action = OrganizationProfileConfirmationAction.LeaveOrganization,
          organizationName = "Acme Inc.",
          state = OrganizationProfileActionConfirmationState(),
          actions = noOpActionConfirmationActions,
        )
      }
    }
  }

  @Test
  fun organizationActionDeleteReady() {
    snapshot {
      ActionSnapshotSurface {
        OrganizationProfileActionConfirmationContent(
          action = OrganizationProfileConfirmationAction.DeleteOrganization,
          organizationName = "Acme Inc.",
          state = OrganizationProfileActionConfirmationState(confirmationText = "Acme Inc."),
          actions = noOpActionConfirmationActions,
        )
      }
    }
  }

  @Test
  fun organizationActionLeaveLoading() {
    snapshot {
      ActionSnapshotSurface {
        OrganizationProfileActionConfirmationContent(
          action = OrganizationProfileConfirmationAction.LeaveOrganization,
          organizationName = "Acme Inc.",
          state =
            OrganizationProfileActionConfirmationState(
              confirmationText = "Acme Inc.",
              isLoading = true,
            ),
          actions = noOpActionConfirmationActions,
        )
      }
    }
  }

  @Test
  fun organizationActionDeleteError() {
    snapshot {
      ActionSnapshotSurface {
        OrganizationProfileActionConfirmationContent(
          action = OrganizationProfileConfirmationAction.DeleteOrganization,
          organizationName = "Acme Inc.",
          state =
            OrganizationProfileActionConfirmationState(
              confirmationText = "Acme Inc.",
              errorMessage = "Unable to delete organization",
            ),
          actions = noOpActionConfirmationActions,
        )
      }
    }
  }

  private val allMembersTabs =
    listOf(
      OrganizationMembersTab.Members,
      OrganizationMembersTab.Invitations,
      OrganizationMembersTab.Requests,
    )

  private val sampleRoles =
    listOf(snapshotRole("org:admin", "Admin"), snapshotRole("org:member", "Member"))

  private val noOpMembersActions =
    OrganizationMembersActions(
      onRetry = {},
      onSelectTab = {},
      onMemberSearchChanged = {},
      onSubmitMemberSearch = {},
      onLoadMoreMembers = {},
      onLoadMoreInvitations = {},
      onLoadMoreRequests = {},
      onUpdateMemberRole = { _, _ -> },
      onRemoveMember = {},
      onRevokeInvitation = {},
      onAcceptRequest = {},
      onRejectRequest = {},
    )

  private val noOpDomainActions =
    OrganizationVerifiedDomainsActions(
      onRetry = {},
      onLoadMore = {},
      onShowAddDomain = {},
      onShowVerifyEmail = {},
      onShowEnrollmentMode = {},
      onShowDeleteDomain = {},
      onDismissFlow = {},
      onDomainNameChanged = {},
      onCreateDomain = {},
      onAffiliationEmailLocalPartChanged = {},
      onSendAffiliationEmail = {},
      onVerificationCodeChanged = {},
      onVerifyCode = {},
      onResendVerificationCode = { _, _ -> },
      onSelectEnrollmentMode = {},
      onUpdateEnrollmentMode = {},
      onDeleteDomain = {},
    )

  private val noOpActionConfirmationActions =
    OrganizationProfileActionConfirmationActions(
      onConfirmationTextChanged = {},
      onConfirm = {},
      onCancel = {},
    )

  private fun sampleMember(
    id: String,
    firstName: String,
    lastName: String,
  ): OrganizationMembership {
    val member = snapshotMembership(roleName = "Member")
    every { member.id } returns id
    every { member.permissions } returns emptyList()
    every { member.publicUserData } returns samplePublicUserData(id, firstName, lastName)
    every { member.createdAt } returns Instant.ofEpochMilli(SAMPLE_JOINED_AT)
    return member
  }

  private fun sampleInvitation(id: String): OrganizationInvitation =
    mockk(relaxed = true) {
      every { this@mockk.id } returns id
      every { emailAddress } returns "new.member@example.com"
      every { role } returns "org:member"
      every { status } returns OrganizationInvitationStatus.Pending
    }

  private fun sampleRequest(id: String): OrganizationMembershipRequest =
    mockk(relaxed = true) {
      every { this@mockk.id } returns id
      every { publicUserData } returns samplePublicUserData(id, "Grace", "Hopper")
      every { status } returns OrganizationInvitationStatus.Pending
    }

  private fun sampleDomain(
    id: String,
    name: String,
    verified: Boolean = true,
    enrollmentMode: String = "manual_invitation",
  ): OrganizationDomain =
    mockk(relaxed = true) {
      every { this@mockk.id } returns id
      every { this@mockk.name } returns name
      every { this@mockk.enrollmentMode } returns
        when (enrollmentMode) {
          "automatic_suggestion" -> OrganizationEnrollmentMode.AutomaticSuggestion
          "automatic_invitation" -> OrganizationEnrollmentMode.AutomaticInvitation
          else -> OrganizationEnrollmentMode.ManualInvitation
        }
      every { verification } returns
        OrganizationDomainVerification(
          if (verified) OrganizationDomainVerificationStatus.Verified
          else OrganizationDomainVerificationStatus.Unverified,
          0.0,
          Instant.parse("2030-01-01T00:00:00Z"),
        )
      every { affiliationEmailAddress } returns null
    }

  private fun samplePublicUserData(
    id: String,
    firstName: String,
    lastName: String,
  ): PublicUserData {
    return PublicUserData(
      firstName = firstName,
      lastName = lastName,
      imageUrl = "",
      hasImage = false,
      identifier = "${firstName.lowercase()}@example.com",
      userId = "user_$id",
    )
  }
}

private const val SAMPLE_JOINED_AT = 1746446400000L

@Composable
private fun MembersSnapshotSurface(content: @Composable () -> Unit) {
  ClerkMaterialTheme {
    Box(modifier = Modifier.size(740.dp).background(ClerkMaterialTheme.colors.background)) {
      content()
    }
  }
}

@Composable
private fun DomainsSnapshotSurface(content: @Composable () -> Unit) {
  ClerkMaterialTheme {
    Box(modifier = Modifier.size(740.dp).background(ClerkMaterialTheme.colors.background)) {
      content()
    }
  }
}

@Composable
private fun ActionSnapshotSurface(content: @Composable () -> Unit) {
  ClerkMaterialTheme {
    Box(modifier = Modifier.size(740.dp).background(ClerkMaterialTheme.colors.background)) {
      content()
    }
  }
}
