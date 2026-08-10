package com.clerk.ui.userprofile.email

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.network.model.verification.Verification
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlin.test.Test
import kotlin.test.assertEquals

class UserProfileEmailRowTest : BaseSnapshotTest() {

  @Test
  fun navigationTransition_rendersWithoutInteractiveState() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        UserProfileEmailRow(
          emailAddress =
            EmailAddress(
              id = "email_1",
              emailAddress = "user@example.com",
              verification = Verification(Verification.Status.VERIFIED),
            ),
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

    paparazzi.snapshot {
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
