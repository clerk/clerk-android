package com.clerk.prebuiltui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.prebuiltui.ui.theme.ClerkTheme
import com.clerk.ui.R as ClerkUiR
import com.clerk.ui.auth.AuthView
import com.clerk.ui.organizationlist.OrganizationListView
import com.clerk.ui.organizationprofile.OrganizationProfileView
import com.clerk.ui.organizationprofile.custom.LocalOrganizationProfileCustomNavigator
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRowPlacement
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRowIcon
import com.clerk.ui.organizationswitcher.OrganizationSwitcher

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle()
      val isAuthFlowComplete by Clerk.isAuthFlowCompleteFlow.collectAsStateWithLifecycle()
      val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
      ClerkTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
          ) {
            if (isInitialized) {
              if (!isAuthFlowComplete) {
                AuthView(isDismissible = false)
              } else {
                SignedInPrebuiltHome(
                  hasActiveOrganization = session?.lastActiveOrganizationId != null
                )
              }
            } else {
              CircularProgressIndicator()
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SignedInPrebuiltHome(hasActiveOrganization: Boolean) {
  var showOrganizationList by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
  var showOrganizationProfile by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
  val customRows = rememberOrganizationSampleCustomRows()

  OrganizationSampleLauncherContent(
    hasActiveOrganization = hasActiveOrganization,
    customRows = customRows,
    onShowOrganizationList = { showOrganizationList = true },
    onShowOrganizationProfile = { showOrganizationProfile = true },
  )

  OrganizationSampleDialogs(
    showOrganizationList = showOrganizationList,
    showOrganizationProfile = showOrganizationProfile,
    customRows = customRows,
    onDismissOrganizationList = { showOrganizationList = false },
    onDismissOrganizationProfile = { showOrganizationProfile = false },
  )
}

@Composable
private fun rememberOrganizationSampleCustomRows(): List<OrganizationProfileCustomRow> {
  val billingLabel = stringResource(R.string.billing)
  val supportLabel = stringResource(R.string.support)
  return remember(billingLabel, supportLabel) {
    listOf(
      OrganizationProfileCustomRow(
        routeKey = BILLING_ROUTE,
        title = billingLabel,
        icon = OrganizationProfileRowIcon.Resource(ClerkUiR.drawable.ic_credit_card),
        placement = OrganizationProfileCustomRowPlacement.After(OrganizationProfileRow.Members),
      ),
      OrganizationProfileCustomRow(
        routeKey = SUPPORT_ROUTE,
        title = supportLabel,
        icon = OrganizationProfileRowIcon.Resource(ClerkUiR.drawable.ic_information_circle),
        placement =
          OrganizationProfileCustomRowPlacement.Before(OrganizationProfileRow.LeaveOrganization),
      ),
    )
  }
}

@Composable
private fun OrganizationSampleLauncherContent(
  hasActiveOrganization: Boolean,
  customRows: List<OrganizationProfileCustomRow>,
  onShowOrganizationList: () -> Unit,
  onShowOrganizationProfile: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    OrganizationSwitcher(
      organizationProfileCustomRows = customRows,
      organizationProfileCustomDestination = { routeKey ->
        SampleOrganizationProfileDestination(routeKey = routeKey)
      },
    )
    Text(
      text = stringResource(R.string.organization_samples_title),
      style = MaterialTheme.typography.headlineSmall,
    )
    Button(modifier = Modifier.fillMaxWidth(), onClick = onShowOrganizationList) {
      Text(text = stringResource(R.string.open_organization_list))
    }
    OutlinedButton(
      modifier = Modifier.fillMaxWidth(),
      enabled = hasActiveOrganization,
      onClick = onShowOrganizationProfile,
    ) {
      Text(text = stringResource(R.string.open_organization_profile))
    }
  }
}

@Composable
private fun OrganizationSampleDialogs(
  showOrganizationList: Boolean,
  showOrganizationProfile: Boolean,
  customRows: List<OrganizationProfileCustomRow>,
  onDismissOrganizationList: () -> Unit,
  onDismissOrganizationProfile: () -> Unit,
) {
  if (showOrganizationList) {
    FullScreenPrebuiltDialog(onDismissRequest = onDismissOrganizationList) {
      OrganizationListView(
        modifier = Modifier.fillMaxSize(),
        onDismissRequest = onDismissOrganizationList,
        onAccountSelected = { onDismissOrganizationList() },
      )
    }
  }

  if (showOrganizationProfile) {
    FullScreenPrebuiltDialog(onDismissRequest = onDismissOrganizationProfile) {
      OrganizationProfileView(
        modifier = Modifier.fillMaxSize(),
        customRows = customRows,
        customDestination = { routeKey ->
          SampleOrganizationProfileDestination(routeKey = routeKey)
        },
        onDismiss = onDismissOrganizationProfile,
      )
    }
  }
}

@Composable
private fun FullScreenPrebuiltDialog(
  onDismissRequest: () -> Unit,
  content: @Composable () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Surface(modifier = Modifier.fillMaxSize()) { content() }
  }
}

@Composable
private fun SampleOrganizationProfileDestination(routeKey: String) {
  val navigator = LocalOrganizationProfileCustomNavigator.current
  val title =
    when (routeKey) {
      BILLING_ROUTE -> stringResource(R.string.billing_destination_title)
      SUPPORT_ROUTE -> stringResource(R.string.support_destination_title)
      else -> routeKey
    }

  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(text = title, style = MaterialTheme.typography.headlineSmall)
    Text(text = stringResource(R.string.custom_destination_body))
    Button(onClick = navigator::navigateBack) { Text(text = stringResource(R.string.done)) }
  }
}

private const val BILLING_ROUTE = "billing"
private const val SUPPORT_ROUTE = "support"
