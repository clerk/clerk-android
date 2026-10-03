package com.clerk.e2e

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.user.User
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

private const val HOST_PREFERENCES = "verify_host"
private const val STORAGE_SCOPE_KEY = "storageScope"
private const val CLERK_PREFERENCES = "clerk_preferences"

class E2EApplication : Application() {
  private var verifyHost: VerifyHost? = null

  fun verifyHost(activity: Activity): VerifyHost =
    verifyHost
      ?: VerifyHost(
          activity = activity,
          config =
            VerifyLaunchConfig.parse(
              extra = { activity.intent.extras?.getString(it) },
              fallbackPublishableKey = BuildConfig.E2E_CLERK_PUBLISHABLE_KEY,
            ),
        )
        .also { verifyHost = it }
}

class VerifyHost(activity: Activity, val config: VerifyLaunchConfig) {
  private val scope = MainScope()
  private val ticket =
    MutableStateFlow(if (config.signInTicket == null) VerifyTicket.None else VerifyTicket.Pending)
  private val lastError = MutableStateFlow(config.publishableKeyFailure ?: config.screenFailure)

  val state: StateFlow<VerifyState>

  init {
    if (config.publishableKeyFailure == null) {
      clearClerkStorageIfScopeChanged(activity)
      Clerk.initialize(
        activity,
        config.publishableKey,
        ClerkConfigurationOptions(enableDebugMode = config.debugLogging),
      )
    }

    val clerk =
      combine(
        Clerk.isInitialized,
        Clerk.isAuthFlowCompleteFlow,
        Clerk.clientFlow,
        Clerk.sessionFlow,
        Clerk.userFlow,
        ::ClerkSnapshot,
      )
    val initial = verifyState(currentClerkSnapshot(), ticket.value, lastError.value)
    Log.i(VERIFY_LOG_TAG, initial.line)
    state =
      combine(clerk, ticket, lastError, ::verifyState)
        .onStart { emit(initial) }
        .distinctUntilChanged()
        .drop(1)
        .onEach { Log.i(VERIFY_LOG_TAG, it.line) }
        .stateIn(scope, SharingStarted.Eagerly, initial)

    scope.launch { launch() }
  }

  private suspend fun launch() {
    val environmentLoaded = config.publishableKeyFailure == null && awaitEnvironment()
    val signInTicket = config.signInTicket ?: return
    ticket.value = if (environmentLoaded) signIn(signInTicket) else VerifyTicket.Failed
  }

  private suspend fun awaitEnvironment(): Boolean {
    val (loaded, error) =
      combine(Clerk.isInitialized, Clerk.initializationError, ::Pair).first { (loaded, error) ->
        loaded || error != null
      }
    if (!loaded) {
      lastError.value = VerifyFailure(code = "environment_load_failed", message = error.toString())
    }
    return loaded
  }

  private suspend fun signIn(signInTicket: String): VerifyTicket =
    when (val result = Clerk.auth.signInWithTicket(signInTicket)) {
      is ClerkResult.Success -> VerifyTicket.Succeeded
      is ClerkResult.Failure -> {
        val error = result.error?.errors?.firstOrNull()
        lastError.value =
          VerifyFailure(
            code = error?.code ?: "ticket_sign_in_failed",
            message =
              error?.longMessage
                ?: error?.message
                ?: result.throwable?.message
                ?: "Ticket sign-in failed.",
          )
        VerifyTicket.Failed
      }
    }

  private fun verifyState(
    clerk: ClerkSnapshot,
    ticket: VerifyTicket,
    lastError: VerifyFailure?,
  ): VerifyState {
    val session = clerk.session
    return VerifyState(
      runId = config.runId,
      launchId = config.launchId,
      screen = renderedScreen(clerk, ticket),
      environmentLoaded = clerk.environmentLoaded,
      userId = clerk.user?.id,
      sessionId = session?.id,
      sessionStatus =
        when (session?.status) {
          Session.SessionStatus.ACTIVE -> "active"
          Session.SessionStatus.PENDING -> "pending"
          else -> null
        },
      pendingTasks = session?.tasks.orEmpty().map { it.key },
      orgId = Clerk.organization?.id,
      signInStatus =
        clerk.client?.signIn?.status?.let { Json.encodeToJsonElement(it).jsonPrimitive.content },
      signUpStatus =
        clerk.client?.signUp?.status?.let { Json.encodeToJsonElement(it).jsonPrimitive.content },
      ticket = ticket,
      lastError = lastError,
    )
  }

  private fun renderedScreen(clerk: ClerkSnapshot, ticket: VerifyTicket): HostScreen =
    when {
      config.publishableKeyFailure != null -> HostScreen.Error
      ticket == VerifyTicket.Pending || !clerk.environmentLoaded -> HostScreen.Launching
      config.screen == VerifyScreen.Auth && clerk.authFlowComplete -> VerifyScreen.Home
      else -> config.screen
    }

  private fun clearClerkStorageIfScopeChanged(context: Context) {
    val storageScope = config.storageScope ?: return
    val preferences = context.getSharedPreferences(HOST_PREFERENCES, Context.MODE_PRIVATE)
    if (preferences.getString(STORAGE_SCOPE_KEY, null) == storageScope) return
    context.deleteSharedPreferences(CLERK_PREFERENCES)
    preferences.edit(commit = true) { putString(STORAGE_SCOPE_KEY, storageScope) }
  }

  private fun currentClerkSnapshot() =
    ClerkSnapshot(
      environmentLoaded = Clerk.isInitialized.value,
      authFlowComplete = Clerk.isAuthFlowComplete,
      client = Clerk.clientFlow.value,
      session = Clerk.sessionFlow.value,
      user = Clerk.userFlow.value,
    )
}

private data class ClerkSnapshot(
  val environmentLoaded: Boolean,
  val authFlowComplete: Boolean,
  val client: Client?,
  val session: Session?,
  val user: User?,
)
