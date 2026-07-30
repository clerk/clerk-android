package com.clerk.ui.userprofile.email

import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.clerk.base.BaseSnapshotTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserProfileEmailRowTest : BaseSnapshotTest() {

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
