package com.clerk.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.organizations.UserOrganizationInvitation
import com.clerk.api.sso.OAuthProvider
import com.clerk.ui.auth.AuthStartViewHelper
import com.clerk.ui.auth.AuthStartViewImpl
import com.clerk.ui.auth.AuthStartViewModel
import com.clerk.ui.auth.FixedAuthStartConfig
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.organizationlist.OrganizationAccountListActions
import com.clerk.ui.organizationlist.OrganizationAccountListContent
import com.clerk.ui.organizationlist.OrganizationAccountListState
import com.clerk.ui.organizationprofile.form.OrganizationProfileFormView
import com.clerk.ui.organizationswitcher.OrganizationSwitcherOverviewSheetContent
import com.clerk.ui.organizationswitcher.previewOrganizationMembership
import com.clerk.ui.signin.password.forgot.AlternativeFactorList
import com.clerk.ui.signup.completeprofile.LegalConsentView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.util.TextIconHelper
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClerkTestTagsTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @After
  fun tearDown() {
    composeTestRule.activityRule.scenario.close()
  }

  @Test
  fun `auth start tags each control on its interactive node`() {
    showAuthStart()

    composeTestRule.onNodeWithTag(ClerkTestTags.Auth.Start.identifier).assert(hasSetTextAction())
    composeTestRule.onNodeWithTag(ClerkTestTags.Auth.Start.continueButton).assertHasClickAction()
    composeTestRule.onNodeWithTag(ClerkTestTags.dismissButton).assertHasClickAction()
    composeTestRule
      .onNodeWithTag(ClerkTestTags.Auth.socialProviderButton("oauth_google"))
      .assertHasClickAction()
    composeTestRule
      .onNodeWithTag(ClerkTestTags.Auth.socialProviderButton("oauth_apple"))
      .assertHasClickAction()
    composeTestRule
      .onNodeWithTag(ClerkTestTags.Auth.Start.identifierSwitcherButton)
      .assertHasClickAction()
      .performClick()

    composeTestRule.onNodeWithTag(ClerkTestTags.Auth.Start.phoneNumber).assert(hasSetTextAction())
    composeTestRule.onNodeWithTag(ClerkTestTags.Auth.Start.identifier).assertDoesNotExist()
  }

  @Test
  fun `alternative methods are tagged by strategy`() {
    setThemedContent {
      AlternativeFactorList(
        alternativeFactors =
          persistentListOf(Factor(StrategyKeys.EMAIL_CODE), Factor(StrategyKeys.PASSWORD)),
        textIconHelper = TextIconHelper(),
        context = composeTestRule.activity,
        onClickFactor = {},
      )
    }

    composeTestRule
      .onNodeWithTag(ClerkTestTags.Auth.SignIn.alternativeMethodButton(StrategyKeys.EMAIL_CODE))
      .assertHasClickAction()
    composeTestRule
      .onNodeWithTag(ClerkTestTags.Auth.SignIn.alternativeMethodButton(StrategyKeys.PASSWORD))
      .assertHasClickAction()
  }

  @Test
  fun `legal consent tags the toggle`() {
    setThemedContent {
      LegalConsentView(
        isAccepted = false,
        onAcceptedChange = {},
        termsUrl = "https://example.com/terms",
        privacyPolicyUrl = null,
      )
    }

    composeTestRule.onNodeWithTag(ClerkTestTags.Auth.SignUp.legalAccepted).assert(isToggleable())
  }

  @Test
  fun `organization account list tags memberships invitations and create`() {
    setThemedContent {
      OrganizationAccountListContent(
        state =
          OrganizationAccountListState(
            isLoading = false,
            hasLoadedInitialResources = true,
            canCreateOrganization = true,
            memberships =
              listOf(
                previewOrganizationMembership(organizationId = "org_1", organizationName = "One"),
                previewOrganizationMembership(organizationId = "org_2", organizationName = "Two"),
              ),
            membershipsTotalCount = 2,
            invitations =
              listOf(
                invitation(id = "inv_pending", organizationId = "org_3", status = "pending"),
                invitation(id = "inv_accepted", organizationId = "org_4", status = "accepted"),
              ),
            invitationsTotalCount = 1,
          ),
        user = null,
        activeOrganizationId = null,
        header = null,
        showPersonalAccount = false,
        showSelectedAccessory = false,
        actions = noOpActions,
      )
    }

    composeTestRule
      .onAllNodesWithTag(ClerkTestTags.Organization.AccountList.membershipButton)
      .assertCountEquals(2)
    composeTestRule
      .onNodeWithTag(ClerkTestTags.Organization.AccountList.invitationJoinButton)
      .assertHasClickAction()
    composeTestRule
      .onNodeWithTag(ClerkTestTags.Organization.AccountList.acceptedInvitationButton)
      .assertHasClickAction()
    composeTestRule
      .onNodeWithTag(ClerkTestTags.Organization.AccountList.createOrganizationButton)
      .assertHasClickAction()
  }

  @Test
  fun `organization switcher overview tags its actions`() {
    setThemedContent {
      OrganizationSwitcherOverviewSheetContent(
        membership = previewOrganizationMembership(),
        onManageOrganization = {},
        onSwitchAccount = {},
      )
    }

    composeTestRule
      .onNodeWithTag(ClerkTestTags.OrganizationSwitcher.manageOrganizationButton)
      .assertHasClickAction()
    composeTestRule
      .onNodeWithTag(ClerkTestTags.OrganizationSwitcher.switchAccountButton)
      .assertHasClickAction()
  }

  @Test
  fun `organization profile form tags its fields and submit`() {
    setThemedContent {
      OrganizationProfileFormView(
        initialName = "Acme",
        initialSlug = "acme",
        slugEnabled = true,
        submitText = "Continue",
        isLoading = false,
        onSubmit = {},
      )
    }

    composeTestRule
      .onNodeWithTag(ClerkTestTags.Organization.ProfileForm.name)
      .assert(hasSetTextAction())
    composeTestRule
      .onNodeWithTag(ClerkTestTags.Organization.ProfileForm.slug)
      .assert(hasSetTextAction())
    composeTestRule
      .onNodeWithTag(ClerkTestTags.Organization.ProfileForm.submitButton)
      .assertHasClickAction()
  }

  private fun setThemedContent(content: @Composable () -> Unit) {
    composeTestRule.setContent { ClerkMaterialTheme { content() } }
  }

  private fun showAuthStart() {
    val authViewHelper =
      AuthStartViewHelper(
        FixedAuthStartConfig(
          enabledFirstFactorAttributes = listOf("email_address", "phone_number"),
          authenticatableSocialProviders = listOf(OAuthProvider.GOOGLE, OAuthProvider.APPLE),
        )
      )
    composeTestRule.setContent {
      PreviewAuthStateProvider {
        AuthStartViewImpl(
          authViewHelper = authViewHelper,
          isDismissible = true,
          onDismiss = {},
          onAuthComplete = {},
          authStartViewModel = AuthStartViewModel(),
        )
      }
    }
  }

  private fun invitation(id: String, organizationId: String, status: String) =
    UserOrganizationInvitation(
      id = id,
      emailAddress = "user+clerk_test@example.com",
      publicOrganizationData =
        UserOrganizationInvitation.PublicOrganizationData(
          id = organizationId,
          name = organizationId,
          imageUrl = null,
          hasImage = false,
        ),
      publicMetadata = "{}",
      role = "org:member",
      status = status,
      createdAt = 0L,
      updatedAt = 0L,
    )

  private val noOpActions =
    OrganizationAccountListActions(
      onRetryInitialLoad = {},
      onLoadMoreMemberships = {},
      onLoadMoreInvitations = {},
      onLoadMoreSuggestions = {},
      onSelectPersonalAccount = {},
      onSelectOrganization = {},
      onAcceptInvitation = {},
      onAcceptSuggestion = {},
      onCreateOrganization = {},
    )
}
