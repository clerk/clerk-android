package com.clerk.ui.userprofile.mfa

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.clerk.api.Clerk
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.user.phoneNumbersAvailableForMfa
import com.clerk.ui.core.common.scaffold.ClerkThemedProfileScaffold

@Composable fun UserProfileMfaAddSmsView(modifier: Modifier = Modifier) {}

@Composable
fun UserProfileMfaAddSmsViewImpl(modifier: Modifier = Modifier) {
  val snackbarHostState = remember { SnackbarHostState() }
  val availablePhoneNumbers =
    remember(Clerk.user) { Clerk.user?.phoneNumbersAvailableForMfa() ?: emptyList() }
      .filter { it.verification?.status == Verification.Status.VERIFIED }
      .sortedBy { it.createdAt }

  ClerkThemedProfileScaffold() {}
}
