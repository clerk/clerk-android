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
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.clerk.api.Clerk
import com.clerk.api.session.pendingTaskKey
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
import com.clerk.ui.userprofile.ClerkUserProfileRoute
import com.clerk.ui.userprofile.clerkUserProfileEntries
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
      val user by Clerk.userFlow.collectAsStateWithLifecycle()
      ClerkTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
          ) {
            if (user != null && session?.pendingTaskKey == null) {
              SignedInPrebuiltHome(
                hasActiveOrganization = session?.lastActiveOrganizationId != null
              )
            } else {
              AuthView()
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
  var showHostStackProfile by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
  val customRows = rememberOrganizationSampleCustomRows()

  OrganizationSampleLauncherContent(
    hasActiveOrganization = hasActiveOrganization,
    customRows = customRows,
    onShowOrganizationList = { showOrganizationList = true },
    onShowOrganizationProfile = { showOrganizationProfile = true },
    onShowHostStackProfile = { showHostStackProfile = true },
  )

  OrganizationSampleDialogs(
    showOrganizationList = showOrganizationList,
    showOrganizationProfile = showOrganizationProfile,
    customRows = customRows,
    onDismissOrganizationList = { showOrganizationList = false },
    onDismissOrganizationProfile = { showOrganizationProfile = false },
  )

  if (showHostStackProfile) {
    FullScreenPrebuiltDialog(onDismissRequest = { showHostStackProfile = false }) {
      HostStackProfileSample(onClose = { showHostStackProfile = false })
    }
  }
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
  onShowHostStackProfile: () -> Unit,
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
    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onShowHostStackProfile) {
      Text(text = stringResource(R.string.open_host_stack_profile))
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

/**
 * Demonstrates hosting Clerk's user profile inside the app's own NavDisplay: the app owns the back
 * stack and transitions, and Clerk's screens are ordinary destinations on it.
 */
@Composable
private fun HostStackProfileSample(onClose: () -> Unit) {
  val backStack = rememberNavBackStack(HostHomeRoute)

  NavDisplay(
    modifier = Modifier.fillMaxSize(),
    backStack = backStack,
    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() else onClose() },
    entryProvider =
      entryProvider {
        entry<HostHomeRoute> {
          Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = stringResource(R.string.host_stack_home_title),
              style = MaterialTheme.typography.headlineSmall,
            )
            Button(onClick = { backStack.add(ClerkUserProfileRoute) }) {
              Text(text = stringResource(R.string.open_clerk_profile))
            }
            OutlinedButton(onClick = onClose) { Text(text = stringResource(R.string.done)) }
          }
        }

        clerkUserProfileEntries(backStack)
      },
  )
}

@Serializable private data object HostHomeRoute : NavKey

private const val BILLING_ROUTE = "billing"
private const val SUPPORT_ROUTE = "support"
