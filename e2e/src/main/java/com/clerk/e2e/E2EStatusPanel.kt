package com.clerk.e2e

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.clerk.api.session.Session
import com.clerk.api.user.User

@Composable
internal fun E2EStatusPanel(
  user: User?,
  session: Session?,
  cleanupStatus: CleanupStatus,
  totpState: TotpTypingState,
  onTypeTotpCode: () -> Unit,
  modifier: Modifier = Modifier,
  showTotpHelper: Boolean = false,
) {
  Column(
    modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      if (user == null) {
        Marker("Signed out", E2ETags.SIGNED_OUT)
      } else {
        Marker("Signed in", E2ETags.SIGNED_IN)
        Marker(user.id, E2ETags.USER_ID)
      }
      SessionMarkers(session)
      CleanupMarker(cleanupStatus)
    }
    if (showTotpHelper) {
      OutlinedButton(
        modifier = Modifier.testTag(E2ETags.TOTP_TYPE_CODE),
        onClick = onTypeTotpCode,
      ) {
        Text("Type TOTP code")
      }
    }
    TotpMarker(totpState)
  }
}

@Composable
private fun SessionMarkers(session: Session?) {
  if (session == null) return
  Marker(session.status.name.lowercase(), E2ETags.SESSION_STATUS)
  when (session.status) {
    Session.SessionStatus.ACTIVE -> Marker("Session active", E2ETags.SESSION_ACTIVE)
    Session.SessionStatus.PENDING -> Marker("Session pending", E2ETags.SESSION_PENDING)
    else -> Unit
  }
  val taskKeys = (listOfNotNull(session.currentTask) + session.tasks).map { it.key }.distinct()
  if (taskKeys.isNotEmpty()) {
    Marker(taskKeys.joinToString(","), E2ETags.PENDING_TASKS)
  }
}

@Composable
private fun CleanupMarker(status: CleanupStatus) {
  when (status) {
    CleanupStatus.Idle -> Unit
    CleanupStatus.InProgress -> Marker("Cleanup in progress", E2ETags.CLEANUP_IN_PROGRESS)
    CleanupStatus.Complete -> Marker("Cleanup complete", E2ETags.CLEANUP_COMPLETE)
    is CleanupStatus.Failed -> Marker(status.message, E2ETags.CLEANUP_FAILED)
  }
}

@Composable
private fun TotpMarker(state: TotpTypingState) {
  when (state) {
    TotpTypingState.Idle -> Unit
    TotpTypingState.Typed -> Marker("TOTP code typed", E2ETags.TOTP_TYPED)
    is TotpTypingState.Failed -> Marker(state.message, E2ETags.TOTP_FAILED)
  }
}

@Composable
private fun Marker(text: String, tag: String) {
  Text(text = text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag(tag))
}
