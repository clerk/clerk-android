package com.clerk.ui.userprofile.email

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.clerk.api.EmailAddress
import com.clerk.api.VerificationStatus
import com.clerk.base.BaseSnapshotTest
import com.clerk.testing.mockVerification
import com.clerk.ui.theme.ClerkMaterialTheme
import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class UserProfileEmailRowTest : BaseSnapshotTest() {

  @Test
  fun navigationTransition_rendersWithoutInteractiveState() {
    snapshot {
      ClerkMaterialTheme {
        UserProfileEmailRow(
          emailAddress =
            mockk<EmailAddress>(relaxed = true) {
              every { id } returns "email_1"
              every { emailAddress } returns "user@example.com"
              every { verification } returns mockVerification(VerificationStatus.Verified)
            },
          isInteractive = false,
          onError = {},
          onVerify = {},
        )
      }
    }
  }

  @Test
  fun failureState_reportsErrorOnceAcrossRecomposition() {
    val reportedErrors = mutableListOf<String>()
    val callbackVersions = mutableListOf<Int>()
    var recompositionTrigger by mutableIntStateOf(0)

    snapshot {
      val callbackVersion = recompositionTrigger
      ReportEmailRowError(EmailViewModel.State.Failure("Unable to update email")) {
        reportedErrors += it
        callbackVersions += callbackVersion
      }
      Text(recompositionTrigger.toString())
      LaunchedEffect(Unit) { recompositionTrigger++ }
    }

    assertEquals(listOf("Unable to update email"), reportedErrors)
    assertEquals(listOf(0), callbackVersions)
  }
}
