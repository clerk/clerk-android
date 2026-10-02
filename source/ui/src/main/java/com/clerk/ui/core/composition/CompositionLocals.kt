package com.clerk.ui.core.composition

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.api.Clerk
import com.clerk.telemetry.ClerkTelemetryEnvironment
import com.clerk.telemetry.TelemetryCollector
import com.clerk.telemetry.TelemetryModule
import com.clerk.ui.auth.AuthIdentifierConfig
import com.clerk.ui.auth.AuthMode
import com.clerk.ui.auth.AuthState
import com.clerk.ui.auth.authSharedPreferences

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalAuthState = staticCompositionLocalOf<AuthState> { error("No AuthState provided") }

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalTelemetryCollector =
  staticCompositionLocalOf<TelemetryCollector> { error("No telemetry provided") }

/**
 * Custom logo content that replaces the SDK-managed logo in authentication screens. When non-null,
 * it takes precedence over the dashboard-configured logo and the SDK applies no sizing or spacing
 * to it.
 */
@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalClerkLogoContent = staticCompositionLocalOf<(@Composable () -> Unit)?> { null }

/**
 * Provides [logo] as the custom logo content for descendant Clerk UI. A null [logo] preserves any
 * logo content provided by an ancestor.
 */
@Composable
internal fun ClerkLogoProvider(logo: (@Composable () -> Unit)?, content: @Composable () -> Unit) {
  val effectiveLogo = logo ?: LocalClerkLogoContent.current
  CompositionLocalProvider(LocalClerkLogoContent provides effectiveLogo, content = content)
}

@Composable
internal fun TelemetryProvider(content: @Composable () -> Unit) {
  val collector = SharedTelemetryCollector.get(LocalContext.current)
  CompositionLocalProvider(LocalTelemetryCollector provides collector, content = content)
}

/** Process-wide because a collector's HTTP client and flush scope are never closed. */
private object SharedTelemetryCollector {
  @Volatile private var instance: TelemetryCollector? = null

  fun get(context: Context): TelemetryCollector =
    instance
      ?: synchronized(this) {
        instance
          ?: TelemetryModule.createCollector(
              context = context.applicationContext,
              environment = ClerkTelemetryEnvironment(),
            )
            .also { instance = it }
      }
}

@Composable
internal fun AuthStateProvider(
  backStack: NavBackStack<NavKey>,
  mode: AuthMode = AuthMode.SignInOrUp,
  identifierConfig: AuthIdentifierConfig = AuthIdentifierConfig(),
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current.applicationContext
  val sharedPreferences = remember(context) { authSharedPreferences(context) }
  val authState =
    remember(backStack, sharedPreferences, mode) {
      AuthState(
        mode = mode,
        backStack = backStack,
        sharedPreferences = sharedPreferences,
        identifierConfig = identifierConfig,
        organizationLogoUrl = Clerk.organizationLogoUrlFlow.value,
      )
    }

  LaunchedEffect(identifierConfig) { authState.applyIdentifierConfig(identifierConfig) }
  LaunchedEffect(authState) {
    Clerk.organizationLogoUrlFlow.collect { authState.updateOrganizationLogoUrl(it) }
  }
  TelemetryProvider { CompositionLocalProvider(LocalAuthState provides authState) { content() } }
}
