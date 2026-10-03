package com.clerk.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

internal fun Modifier.clerkTestTagsAsResourceIds(): Modifier = semantics {
  testTagsAsResourceId = true
}

internal object ClerkTestTags {
  const val dismissButton = "clerk.dismissButton"

  object UserButton {
    const val profileButton = "clerk.userButton.profile"
  }

  object AccountSwitcher {
    fun sessionButton(userId: String) = "clerk.accountSwitcher.session.$userId"

    const val addAccountButton = "clerk.accountSwitcher.addAccount"
    const val signOutAllButton = "clerk.accountSwitcher.signOutAll"
  }

  object UserProfile {
    object Row {
      const val manageAccount = "clerk.userProfile.row.manageAccount"
      const val security = "clerk.userProfile.row.security"
      const val switchAccount = "clerk.userProfile.row.switchAccount"
      const val addAccount = "clerk.userProfile.row.addAccount"
      const val signOut = "clerk.userProfile.row.signOut"
    }
  }

  object Auth {
    fun socialProviderButton(strategy: String) = "clerk.auth.socialProvider.$strategy"

    object Start {
      const val identifier = "clerk.auth.start.identifier"
      const val phoneNumber = "clerk.auth.start.phoneNumber"
      const val continueButton = "clerk.auth.start.continue"
      const val biometricSignInButton = "clerk.auth.start.biometricSignIn"
      const val identifierSwitcherButton = "clerk.auth.start.identifierSwitcher"
    }

    object SignIn {
      const val code = "clerk.auth.signIn.code"
      const val password = "clerk.auth.signIn.password"
      const val continueButton = "clerk.auth.signIn.continue"
      const val useAnotherMethodButton = "clerk.auth.signIn.useAnotherMethod"

      fun alternativeMethodButton(strategy: String) =
        "clerk.auth.signIn.alternativeMethod.$strategy"
    }

    object SignUp {
      const val code = "clerk.auth.signUp.code"
      const val emailAddress = "clerk.auth.signUp.emailAddress"
      const val username = "clerk.auth.signUp.username"
      const val password = "clerk.auth.signUp.password"
      const val continueButton = "clerk.auth.signUp.continue"
      const val completeProfileFirstName = "clerk.auth.signUp.completeProfile.firstName"
      const val completeProfileLastName = "clerk.auth.signUp.completeProfile.lastName"
      const val completeProfileContinueButton = "clerk.auth.signUp.completeProfile.continue"
      const val legalAccepted = "clerk.auth.signUp.legalAccepted"
    }

    object SessionTask {
      object SetupMfa {
        const val smsCode = "clerk.auth.sessionTask.setupMfa.smsCode"
        const val authenticatorApp = "clerk.auth.sessionTask.setupMfa.authenticatorApp"
      }

      object ResetPassword {
        const val newPassword = "clerk.auth.sessionTask.resetPassword.newPassword"
        const val confirmPassword = "clerk.auth.sessionTask.resetPassword.confirmPassword"
        const val submitButton = "clerk.auth.sessionTask.resetPassword.submit"
      }
    }
  }

  object Organization {
    object AccountList {
      const val createOrganizationButton = "clerk.organization.accountList.createOrganization"
      const val acceptedInvitationButton = "clerk.organization.accountList.invitation.accepted"
      const val invitationJoinButton = "clerk.organization.accountList.invitation.join"
      const val membershipButton = "clerk.organization.accountList.membership"
      const val personalAccountButton = "clerk.organization.accountList.personalAccount"
    }

    object ProfileForm {
      const val name = "clerk.organization.profileForm.name"
      const val slug = "clerk.organization.profileForm.slug"
      const val submitButton = "clerk.organization.profileForm.submit"
    }
  }

  object OrganizationSwitcher {
    const val triggerButton = "clerk.organizationSwitcher.trigger"
    const val manageOrganizationButton = "clerk.organizationSwitcher.manageOrganization"
    const val switchAccountButton = "clerk.organizationSwitcher.switchAccount"
  }
}
