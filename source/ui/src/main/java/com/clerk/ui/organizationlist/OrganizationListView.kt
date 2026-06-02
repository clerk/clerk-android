package com.clerk.ui.organizationlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationCreationDefaults
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.user.User
import com.clerk.telemetry.TelemetryEvents
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.core.composition.TelemetryProvider
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.footer.DevelopmentModeWarningBox
import com.clerk.ui.organizationprofile.create.OrganizationCreateFlowView
import com.clerk.ui.organizationprofile.invite.OrganizationInviteMembersView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider

/**
 * Standalone account and organization picker.
 *
 * The list includes the signed-in user's personal account when allowed, organization memberships,
 * pending invitations, suggested organizations, and a create-organization action when creation is
 * enabled for the current user.
 *
 * @param clerkTheme Optional theme customization for the list UI.
 * @param hidePersonalAccount Hides the personal account row even when personal account selection is
 *   allowed.
 * @param isDismissable Shows a top dismiss affordance and calls [onDismissRequest] after a
 *   successful selection when possible.
 * @param skipPostCreateInviteFlow Skips the post-create invitation step in the default
 *   create-organization flow.
 * @param onDismissRequest Called when the dismiss affordance is pressed or a dismissable selection
 *   completes.
 * @param onCreateOrganization Optional callback for apps that own create-organization navigation.
 *   When `null`, the list opens Clerk's default create-organization flow.
 * @param onAccountSelected Called after selecting the personal account (`null`) or an organization
 *   ID.
 */
@Composable
@Suppress("LongParameterList", "UNUSED_PARAMETER")
fun OrganizationListView(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  hidePersonalAccount: Boolean = false,
  isDismissable: Boolean = true,
  skipPostCreateInviteFlow: Boolean = false,
  onDismissRequest: (() -> Unit)? = null,
  onCreateOrganization: ((OrganizationCreationDefaults?) -> Unit)? = null,
  onAccountSelected: ((String?) -> Unit)? = null,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    ClerkMaterialTheme {
      TelemetryProvider {
        DevelopmentModeWarningBox(modifier = modifier.fillMaxSize()) {
          OrganizationListViewImpl(
            modifier = Modifier.fillMaxSize(),
            clerkTheme = clerkTheme,
            hidePersonalAccount = hidePersonalAccount,
            isDismissable = isDismissable,
            skipPostCreateInviteFlow = skipPostCreateInviteFlow,
            onDismissRequest = onDismissRequest,
            onCreateOrganization = onCreateOrganization,
            onAccountSelected = onAccountSelected,
          )
        }
      }
    }
  }
}

@Composable
@Suppress("LongParameterList")
internal fun OrganizationListViewImpl(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  hidePersonalAccount: Boolean = false,
  isDismissable: Boolean = true,
  skipPostCreateInviteFlow: Boolean = false,
  onDismissRequest: (() -> Unit)? = null,
  onCreateOrganization: ((OrganizationCreationDefaults?) -> Unit)? = null,
  onAccountSelected: ((String?) -> Unit)? = null,
  viewModel: OrganizationAccountListViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val telemetry = LocalTelemetryCollector.current
  val createFlowState = remember { OrganizationListCreateFlowState() }
  val createOrganizationAction =
    onCreateOrganization
      ?: { creationDefaults: OrganizationCreationDefaults? ->
        createFlowState.openCreate(creationDefaults)
      }
  val callbacks =
    OrganizationListCallbacks(
      onDismissRequest = onDismissRequest,
      onCreateOrganization = createOrganizationAction,
      onAccountSelected = onAccountSelected,
    )
  val chrome =
    OrganizationListChrome(
      clerkTheme = clerkTheme,
      isDismissable = isDismissable,
      snackbarHostState = snackbarHostState,
      callbacks = callbacks,
    )
  val showPersonalAccount =
    user != null && !hidePersonalAccount && !Clerk.organizationSelectionIsForced

  LaunchedEffect(user?.id, session?.id) {
    viewModel.reset()
    viewModel.load()
  }
  LaunchedEffect(user?.id) {
    if (user != null) telemetry.record(TelemetryEvents.viewDidAppear("OrganizationListView"))
  }
  LaunchedEffect(state.errorMessage) {
    state.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearError()
    }
  }

  OrganizationListScaffold(chrome = chrome, modifier = modifier) { innerPadding ->
    OrganizationListBody(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      state = state,
      user = user,
      activeOrganizationId = session?.lastActiveOrganizationId,
      showPersonalAccount = showPersonalAccount,
      actions = organizationListActions(state = state, viewModel = viewModel, chrome = chrome),
    )
  }

  OrganizationListCreateFlowDialogs(
    state = createFlowState,
    skipPostCreateInviteFlow = skipPostCreateInviteFlow,
  )
}

@Composable
private fun OrganizationListCreateFlowDialogs(
  state: OrganizationListCreateFlowState,
  skipPostCreateInviteFlow: Boolean,
) {
  if (state.showOrganizationCreate) {
    OrganizationListFullScreenPage(onDismiss = state::dismissCreate) {
      OrganizationCreateFlowView(
        modifier = Modifier.fillMaxSize(),
        creationDefaults = state.organizationCreateDefaults,
        skipInvitationScreen = skipPostCreateInviteFlow,
        onComplete = state::dismissCreate,
        onInviteMembers = state::showPostCreateInvitations,
        onBackPressed = state::dismissCreate,
      )
    }
  }

  state.postCreateInviteOrganization?.let { organization ->
    OrganizationListFullScreenPage(onDismiss = state::dismissPostCreateInvitations) {
      OrganizationInviteMembersView(
        modifier = Modifier.fillMaxSize(),
        organization = organization,
        onComplete = state::dismissPostCreateInvitations,
      )
    }
  }
}

private data class OrganizationListCallbacks(
  val onDismissRequest: (() -> Unit)?,
  val onCreateOrganization: (OrganizationCreationDefaults?) -> Unit,
  val onAccountSelected: ((String?) -> Unit)?,
)

private class OrganizationListCreateFlowState {
  var showOrganizationCreate by mutableStateOf(false)
    private set

  var organizationCreateDefaults by mutableStateOf<OrganizationCreationDefaults?>(null)
    private set

  var postCreateInviteOrganization by mutableStateOf<Organization?>(null)
    private set

  fun openCreate(creationDefaults: OrganizationCreationDefaults?) {
    organizationCreateDefaults = creationDefaults
    showOrganizationCreate = true
  }

  fun dismissCreate() {
    showOrganizationCreate = false
    organizationCreateDefaults = null
  }

  fun showPostCreateInvitations(organization: Organization) {
    dismissCreate()
    postCreateInviteOrganization = organization
  }

  fun dismissPostCreateInvitations() {
    postCreateInviteOrganization = null
  }
}

@Composable
private fun OrganizationListFullScreenPage(onDismiss: () -> Unit, content: @Composable () -> Unit) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = ClerkMaterialTheme.colors.background,
      contentColor = ClerkMaterialTheme.colors.foreground,
    ) {
      DevelopmentModeWarningBox(modifier = Modifier.fillMaxSize()) { content() }
    }
  }
}

private data class OrganizationListChrome(
  val clerkTheme: ClerkTheme?,
  val isDismissable: Boolean,
  val snackbarHostState: SnackbarHostState,
  val callbacks: OrganizationListCallbacks,
)

@Composable
private fun OrganizationListScaffold(
  chrome: OrganizationListChrome,
  modifier: Modifier = Modifier,
  content: @Composable (PaddingValues) -> Unit,
) {
  Scaffold(
    modifier = modifier,
    snackbarHost = { ClerkErrorSnackbar(chrome.snackbarHostState) },
    topBar = {
      ClerkTopAppBar(
        onBackPressed = { chrome.callbacks.onDismissRequest?.invoke() },
        hasLogo = false,
        hasBackButton = chrome.isDismissable && chrome.callbacks.onDismissRequest != null,
        backgroundColor = ClerkMaterialTheme.colors.background,
        clerkTheme = chrome.clerkTheme,
      )
    },
    containerColor = ClerkMaterialTheme.colors.background,
    content = content,
  )
}

@Composable
private fun OrganizationListBody(
  state: OrganizationAccountListState,
  user: User?,
  activeOrganizationId: String?,
  showPersonalAccount: Boolean,
  actions: OrganizationAccountListActions,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    if (state.isLoading) {
      CircularProgressIndicator()
    } else {
      OrganizationAccountListContent(
        modifier = Modifier.fillMaxSize(),
        state = state,
        user = user,
        activeOrganizationId = activeOrganizationId,
        header =
          OrganizationAccountListHeader(
            title = stringResource(R.string.choose_an_account),
            subtitle = stringResource(R.string.select_the_account_with_which_you_wish_to_continue),
          ),
        showPersonalAccount = showPersonalAccount,
        showSelectedAccessory = true,
        contentPadding = PaddingValues(horizontal = dp16, vertical = dp16),
        actions = actions,
      )
    }
  }
}

private fun organizationListActions(
  state: OrganizationAccountListState,
  viewModel: OrganizationAccountListViewModel,
  chrome: OrganizationListChrome,
): OrganizationAccountListActions {
  return OrganizationAccountListActions(
    onRetryInitialLoad = viewModel::retryLoad,
    onLoadMoreMemberships = viewModel::loadMoreMemberships,
    onLoadMoreInvitations = viewModel::loadMoreInvitations,
    onLoadMoreSuggestions = viewModel::loadMoreSuggestions,
    onSelectPersonalAccount = {
      viewModel.selectPersonalAccount {
        chrome.callbacks.onAccountSelected?.invoke(null)
        if (chrome.isDismissable) chrome.callbacks.onDismissRequest?.invoke()
      }
    },
    onSelectOrganization = { organizationId ->
      viewModel.selectOrganization(organizationId) {
        chrome.callbacks.onAccountSelected?.invoke(organizationId)
        if (chrome.isDismissable) chrome.callbacks.onDismissRequest?.invoke()
      }
    },
    onAcceptInvitation = viewModel::acceptInvitation,
    onAcceptSuggestion = viewModel::acceptSuggestion,
    onCreateOrganization = { chrome.callbacks.onCreateOrganization(state.creationDefaults) },
  )
}
