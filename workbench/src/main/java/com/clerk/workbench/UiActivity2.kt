package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.organizationlist.OrganizationListView
import com.clerk.ui.organizationprofile.OrganizationProfileView
import com.clerk.workbench.ui.theme.Background
import com.clerk.workbench.ui.theme.BackgroundDark
import com.clerk.workbench.ui.theme.WorkbenchTheme

class UiActivity2 : ComponentActivity() {

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle =
        SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
    )
    setContent {
      val backgroundColor = if (isSystemInDarkTheme()) BackgroundDark else Background
      WorkbenchTheme {
        Column(modifier = Modifier.fillMaxSize().background(color = Color(0xFFF9F9F9))) {
          WorkbenchAuthGate() {
            Column(
              modifier =
                Modifier.background(color = backgroundColor).fillMaxSize().statusBarsPadding()
            ) {
              val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
              if (session?.lastActiveOrganizationId == null || Clerk.organization == null) {
                OrganizationListView(
                  modifier = Modifier.fillMaxSize(),
                  hidePersonalAccount = true,
                  isDismissible = false,
                )
              } else {
                OrganizationProfileView(isDismissible = true)
              }
            }
          }
        }
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewMainContent() {
  WorkbenchTheme {}
}
