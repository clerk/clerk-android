package com.clerk.e2e

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.session.Session
import com.clerk.api.user.User
import com.clerk.ui.auth.AuthMode
import com.clerk.ui.auth.AuthView
import com.clerk.ui.organizationswitcher.OrganizationSwitcher
import com.clerk.ui.userbutton.UserButton
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: E2EViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val host = (application as E2EApplication).verifyHost(this)
    setContent {
      if (host.config.hasVerifyInputs || host.config.publishableKeyFailure != null) {
        VerifyApp(host)
      } else {
        TrailApp(viewModel)
      }
    }
  }
}

@Composable
private fun VerifyApp(host: VerifyHost) {
  val state by host.state.collectAsStateWithLifecycle()

  when (state.screen) {
    HostScreen.Launching -> HostSurface { CircularProgressIndicator() }
    HostScreen.Error -> HostSurface { LaunchError(state.lastError) }
    VerifyScreen.Home -> Home(host.config.authMode, host::open)
    VerifyScreen.Auth -> RootAuthView(host.config.authMode, host.config.initialIdentifier)
    VerifyScreen.CustomSignIn -> HostSurface { CustomSignIn { host.open(VerifyScreen.Home) } }
  }
}

@Composable
private fun RootAuthView(authMode: AuthMode, initialIdentifier: String?) {
  AuthView(
    initialIdentifier = initialIdentifier,
    persistIdentifiers = false,
    preferGoogleOneTap = false,
    isDismissible = false,
    mode = authMode,
  )
}

@Composable
private fun Home(authMode: AuthMode, open: (VerifyScreen) -> Unit) {
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  val activeSession = session?.takeIf { it.status == Session.SessionStatus.ACTIVE }
  val signedInUser = user.takeIf { activeSession != null }
  var showsAuthView by rememberSaveable { mutableStateOf(false) }

  if (showsAuthView) {
    BackHandler { showsAuthView = false }
    AuthView(
      persistIdentifiers = false,
      preferGoogleOneTap = false,
      onDismiss = { showsAuthView = false },
      onAuthComplete = { showsAuthView = false },
      mode = authMode,
    )
  } else {
    HostSurface {
      if (activeSession != null && signedInUser != null) {
        SignedInHome(signedInUser, activeSession)
      } else {
        Text("Signed out", modifier = Modifier.testTag(E2ETags.Auth.SIGNED_OUT))
        TextAction("Sign in", E2ETags.Auth.SIGN_IN) { showsAuthView = true }
        TextAction("Sign in full screen", E2ETags.Auth.SIGN_IN_FULL_SCREEN) {
          open(VerifyScreen.Auth)
        }
        TextAction("Custom sign-in", E2ETags.Home.CUSTOM_SIGN_IN) {
          open(VerifyScreen.CustomSignIn)
        }
      }
    }
  }
}

@Composable
private fun SignedInHome(user: User, session: Session) {
  val scope = rememberCoroutineScope()

  UserButton()
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = user.primaryEmailAddress?.let { "Signed in as ${it.emailAddress}" } ?: "Signed in",
      modifier = Modifier.testTag(E2ETags.Auth.SIGNED_IN),
      textAlign = TextAlign.Center,
    )
    ProvideTextStyle(
      MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
      LabelledId("User ID", user.id, E2ETags.Auth.USER_ID)
      LabelledId("Session ID", session.id, E2ETags.Auth.SESSION_ID)
    }
  }
  OrganizationSwitcher()
  TextAction("Sign out", E2ETags.Auth.SIGN_OUT) { scope.launch { Clerk.auth.signOut() } }
}

// A Material button puts its role on a child node that covers the whole button, and UI Automator
// drivers refuse to tap a node whose area belongs to a child. A clickable text is one node.
@Composable
internal fun TextAction(label: String, tag: String, onClick: () -> Unit) {
  Text(
    text = label,
    modifier =
      Modifier.testTag(tag)
        .clip(CircleShape)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    color = MaterialTheme.colorScheme.primary,
    style = MaterialTheme.typography.labelLarge,
  )
}

@Composable
private fun LabelledId(label: String, id: String, tag: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(label)
    Text(id, modifier = Modifier.testTag(tag))
  }
}

// One text node, because UI Automator gives a Compose node that merges its children no text of its
// own, and a reader of the screen tree needs the reason with the title.
@Composable
private fun LaunchError(failure: VerifyFailure?) {
  Text(
    text =
      buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("Something went wrong") }
        append("\n")
        append(failure?.message.orEmpty())
      },
    modifier = Modifier.testTag(E2ETags.Launch.ERROR),
    textAlign = TextAlign.Center,
  )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun HostSurface(content: @Composable () -> Unit) {
  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
      Box(contentAlignment = Alignment.Center) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
          content()
        }
      }
    }
  }
}
