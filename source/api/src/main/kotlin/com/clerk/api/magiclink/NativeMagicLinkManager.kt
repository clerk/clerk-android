@file:Suppress("TooManyFunctions")

package com.clerk.api.magiclink

import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.Constants.Strategy.EMAIL_LINK
import com.clerk.api.log.ClerkLog
import com.clerk.api.log.SafeUriLog
import com.clerk.api.network.ApiParams
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.magiclink.NativeMagicLinkPrepareRequest
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.RedirectConfiguration
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import java.net.URL
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PENDING_FLOW_TTL_MS = 10 * 60 * 1000L

public interface NativeMagicLinkManager {
  public var attestationProvider: NativeMagicLinkAttestationProvider?

  public suspend fun startEmailLinkSignIn(email: String): ClerkResult<SignIn, NativeMagicLinkError>

  public suspend fun handleMagicLinkDeepLink(
    uri: Uri
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError>

  public suspend fun complete(
    flowId: String,
    approvalToken: String,
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError>
}

public sealed interface NativeMagicLinkAuthResult {
  public data class SignIn(public val signIn: com.clerk.api.signin.SignIn) :
    NativeMagicLinkAuthResult

  public data class SignUp(public val signUp: com.clerk.api.signup.SignUp) :
    NativeMagicLinkAuthResult
}

internal object NativeMagicLinkService : NativeMagicLinkManager {
  private val mutex = Mutex()
  private val pendingFlowStore: PendingNativeMagicLinkStore =
    PersistentPendingNativeMagicLinkStore()

  override var attestationProvider: NativeMagicLinkAttestationProvider? = null

  override suspend fun startEmailLinkSignIn(
    email: String
  ): ClerkResult<SignIn, NativeMagicLinkError> {
    NativeMagicLinkLogger.start()
    val identifier = email.trim()
    val invalidIdentifierFailure =
      if (identifier.isEmpty()) {
        ClerkResult.apiFailure(
          NativeMagicLinkError(reasonCode = NativeMagicLinkReason.INVALID_IDENTIFIER.code)
        )
      } else {
        null
      }

    return invalidIdentifierFailure ?: createAndPrepareEmailLinkSignIn(identifier)
  }

  private suspend fun createAndPrepareEmailLinkSignIn(
    identifier: String
  ): ClerkResult<SignIn, NativeMagicLinkError> {
    val signInResult =
      ClerkApi.signIn.createSignIn(
        mapOf("identifier" to identifier, "locale" to Clerk.locale.value.orEmpty())
      )

    return when (signInResult) {
      is ClerkResult.Failure ->
        ClerkResult.apiFailure(
          signInResult.toNativeMagicLinkError(NativeMagicLinkReason.START_FAILED)
        )
      is ClerkResult.Success -> prepareEmailLinkSignIn(signInResult.value)
    }
  }

  private suspend fun prepareEmailLinkSignIn(
    signIn: SignIn
  ): ClerkResult<SignIn, NativeMagicLinkError> {
    val emailAddressId = signIn.emailLinkAddressId()
    val redirectUri = resolveNativeEmailLinkRedirectUri()
    val missingReason =
      when {
        emailAddressId == null -> NativeMagicLinkReason.EMAIL_LINK_NOT_SUPPORTED
        redirectUri == null -> NativeMagicLinkReason.START_FAILED
        else -> null
      }

    return if (missingReason != null) {
      ClerkResult.apiFailure(NativeMagicLinkError(reasonCode = missingReason.code))
    } else {
      val requiredEmailAddressId = checkNotNull(emailAddressId)
      val requiredRedirectUri = checkNotNull(redirectUri)
      val pkcePair = PkceUtil.generatePair()
      val prepareRequest =
        NativeMagicLinkPrepareRequest(
          emailAddressId = requiredEmailAddressId,
          redirectUri = requiredRedirectUri,
          codeChallenge = pkcePair.challenge,
        )
      when (
        val prepareResult =
          ClerkApi.signIn.prepareSignInFirstFactor(signIn.id, prepareRequest.toFields())
      ) {
        is ClerkResult.Failure ->
          ClerkResult.apiFailure(
            prepareResult.toNativeMagicLinkError(NativeMagicLinkReason.PREPARE_FAILED)
          )
        is ClerkResult.Success -> {
          NativeMagicLinkLogger.prepareSuccess(
            state = PendingNativeMagicLinkState.SIGN_IN,
            flowId = signIn.id,
          )
          persistPendingFlow(
            createPendingFlow(
              codeVerifier = pkcePair.verifier,
              state = PendingNativeMagicLinkState.SIGN_IN,
              flowId = signIn.id,
            )
          )
          ClerkResult.success(prepareResult.value)
        }
      }
    }
  }

  internal suspend fun prepareSignUpEmailLink(
    signUpId: String,
    strategy: SignUp.PrepareVerificationParams.Strategy.EmailLink,
  ): ClerkResult<SignUp, ClerkErrorResponse> {
    val applicationId = Clerk.applicationId
    val redirectUri =
      strategy.redirectUri
        ?: strategy.redirectUrl
        ?: if (applicationId.isNullOrBlank()) null
        else {
          RedirectConfiguration.emailLinkRedirectUrl(
            applicationId = applicationId,
            proxyUrl = Clerk.proxyUrl,
          )
        }

    if (redirectUri.isNullOrBlank()) {
      return ClerkResult.apiFailure(
        ClerkErrorResponse(
          errors =
            listOf(
              Error(
                message = "is invalid",
                longMessage = "redirect_uri is required for native email-link verification",
                code = "native_redirect_uri_required",
              )
            )
        )
      )
    }

    val pkcePair = PkceUtil.generatePair()
    val fields =
      mapOf(
        ApiParams.STRATEGY to EMAIL_LINK,
        ApiParams.REDIRECT_URI to redirectUri,
        ApiParams.CODE_CHALLENGE to pkcePair.challenge,
        ApiParams.CODE_CHALLENGE_METHOD to NativeMagicLinkPrepareRequest.PKCE_METHOD_S256,
      )

    return when (val prepareResult = ClerkApi.signUp.prepareSignUpVerification(signUpId, fields)) {
      is ClerkResult.Failure -> prepareResult
      is ClerkResult.Success -> {
        NativeMagicLinkLogger.prepareSuccess(
          state = PendingNativeMagicLinkState.SIGN_UP,
          flowId = signUpId,
        )
        persistPendingFlow(
          createPendingFlow(
            codeVerifier = pkcePair.verifier,
            state = PendingNativeMagicLinkState.SIGN_UP,
            flowId = signUpId,
          )
        )
        prepareResult
      }
    }
  }

  override suspend fun handleMagicLinkDeepLink(
    uri: Uri
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    NativeMagicLinkLogger.deepLinkReceived(uri)
    val parsed = parseMagicLinkCallback(uri)
    return when (parsed) {
      is ClerkResult.Failure -> {
        val reason = parsed.error?.reasonCode ?: NativeMagicLinkReason.COMPLETE_FAILED.code
        NativeMagicLinkLogger.completeFailure(reason)
        parsed
      }
      is ClerkResult.Success -> complete(parsed.value.flowId, parsed.value.approvalToken)
    }
  }

  override suspend fun complete(
    flowId: String,
    approvalToken: String,
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    val pendingState = lookupPendingFlow(flowId)
    logCompletionRequested(flowId, pendingState)
    return when (pendingState) {
      PendingFlowLookup.None -> nativeMagicLinkFailure(NativeMagicLinkReason.NO_PENDING_FLOW)
      is PendingFlowLookup.Mismatched -> {
        logFlowIdMismatch(pendingState.expectedFlowId, flowId)
        nativeMagicLinkFailure(NativeMagicLinkReason.FLOW_ID_MISMATCH)
      }
      is PendingFlowLookup.Found ->
        createCompletionRunner()
          .complete(flowId = flowId, approvalToken = approvalToken, pending = pendingState.flow)
    }
  }

  internal fun resetForTests() {
    pendingFlowStore.clear()
    attestationProvider = null
  }

  private suspend fun persistPendingFlow(flow: PendingNativeMagicLinkFlow) {
    NativeMagicLinkLogger.pendingFlowSaved(flow)
    mutex.withLock { pendingFlowStore.save(flow) }
  }

  private suspend fun lookupPendingFlow(flowId: String): PendingFlowLookup {
    return mutex.withLock {
      val flow = pendingFlowStore.load()
      when {
        flow == null -> PendingFlowLookup.None
        flow.expiresAtEpochMs <= currentTimeMillis() -> {
          pendingFlowStore.clear()
          PendingFlowLookup.None
        }
        flow.flowId != null && flow.flowId != flowId -> PendingFlowLookup.Mismatched(flow.flowId)
        else -> PendingFlowLookup.Found(flow)
      }
    }
  }

  private fun logCompletionRequested(flowId: String, pendingState: PendingFlowLookup) {
    val details = pendingState.toCompletionRequestLogDetails()
    NativeMagicLinkLogger.completeRequested(
      flowId = flowId,
      pendingLookupOutcome = details.pendingLookupOutcome,
      pendingState = details.pendingState,
      expectedFlowId = details.expectedFlowId,
    )
  }

  private fun createCompletionRunner(): NativeMagicLinkCompletionRunner {
    val clearPendingFlow: suspend () -> Unit = { mutex.withLock { pendingFlowStore.clear() } }
    return NativeMagicLinkCompletionRunner(
      attestationProvider = attestationProvider,
      clearPendingFlow = clearPendingFlow,
      activateCreatedSession = { createdSessionId -> activateCreatedSession(createdSessionId) },
      refreshClientState = { refreshClientState() },
    )
  }

  private suspend fun refreshClientState() {
    when (val clientResult = Client.get()) {
      is ClerkResult.Success -> Clerk.updateClient(clientResult.value)
      is ClerkResult.Failure -> ClerkLog.w("event=native_magic_link_client_refresh_failure")
    }
  }

  private suspend fun activateCreatedSession(
    createdSessionId: String?
  ): ClerkResult<Unit, NativeMagicLinkError> {
    createdSessionId ?: return ClerkResult.success(Unit)
    return when (val activationResult = Clerk.auth.setActive(createdSessionId)) {
      is ClerkResult.Success -> ClerkResult.success(Unit)
      is ClerkResult.Failure -> {
        refreshClientState()
        val isAlreadyActive =
          runCatching { Clerk.client.lastActiveSessionId }.getOrNull() == createdSessionId
        if (isAlreadyActive) {
          ClerkResult.success(Unit)
        } else {
          ClerkResult.apiFailure(
            activationResult.toNativeMagicLinkError(NativeMagicLinkReason.SESSION_ACTIVATION_FAILED)
          )
        }
      }
    }
  }
}

public fun interface NativeMagicLinkAttestationProvider {
  suspend fun attestation(): String?
}

@Serializable
internal data class PendingNativeMagicLinkFlow(
  val codeVerifier: String,
  val state: PendingNativeMagicLinkState,
  val createdAtEpochMs: Long,
  val expiresAtEpochMs: Long,
  val flowId: String? = null,
)

@Serializable
internal enum class PendingNativeMagicLinkState {
  SIGN_IN,
  SIGN_UP,
}

internal interface PendingNativeMagicLinkStore {
  fun save(flow: PendingNativeMagicLinkFlow)

  fun load(): PendingNativeMagicLinkFlow?

  fun clear()
}

internal class PersistentPendingNativeMagicLinkStore(
  private val json: Json = Json { ignoreUnknownKeys = true }
) : PendingNativeMagicLinkStore {
  override fun save(flow: PendingNativeMagicLinkFlow) {
    StorageHelper.saveValue(StorageKey.PENDING_NATIVE_MAGIC_LINK_FLOW, json.encodeToString(flow))
  }

  override fun load(): PendingNativeMagicLinkFlow? {
    val encoded = StorageHelper.loadValue(StorageKey.PENDING_NATIVE_MAGIC_LINK_FLOW) ?: return null
    return runCatching { json.decodeFromString<PendingNativeMagicLinkFlow>(encoded) }
      .getOrElse { error ->
        ClerkLog.w("event=native_magic_link_pending_flow_decode_failure message=${error.message}")
        clear()
        null
      }
  }

  override fun clear() {
    StorageHelper.deleteValue(StorageKey.PENDING_NATIVE_MAGIC_LINK_FLOW)
  }
}

internal data class ParsedMagicLinkDeepLink(val flowId: String, val approvalToken: String)

private sealed interface PendingFlowLookup {
  data object None : PendingFlowLookup

  data class Found(val flow: PendingNativeMagicLinkFlow) : PendingFlowLookup

  data class Mismatched(val expectedFlowId: String?) : PendingFlowLookup
}

private data class CompletionRequestLogDetails(
  val pendingLookupOutcome: String,
  val pendingState: String,
  val expectedFlowId: String,
)

private fun createPendingFlow(
  codeVerifier: String,
  state: PendingNativeMagicLinkState,
  flowId: String,
): PendingNativeMagicLinkFlow {
  val createdAtEpochMs = currentTimeMillis()
  return PendingNativeMagicLinkFlow(
    codeVerifier = codeVerifier,
    state = state,
    createdAtEpochMs = createdAtEpochMs,
    expiresAtEpochMs = createdAtEpochMs + PENDING_FLOW_TTL_MS,
    flowId = flowId,
  )
}

internal fun canHandleNativeMagicLink(uri: Uri?): Boolean {
  return uri?.let {
    queryOrFragmentParam(it, "flow_id") != null ||
      queryOrFragmentParam(it, "approval_token") != null
  } ?: false
}

internal fun parseMagicLinkCallback(
  uri: Uri
): ClerkResult<ParsedMagicLinkDeepLink, NativeMagicLinkError> {
  val flowId = queryOrFragmentParam(uri, "flow_id")
  val approvalToken = queryOrFragmentParam(uri, "approval_token")

  return when {
    flowId.isNullOrBlank() ->
      ClerkResult.apiFailure(
        NativeMagicLinkError(reasonCode = NativeMagicLinkReason.MISSING_FLOW_ID.code)
      )
    approvalToken.isNullOrBlank() ->
      ClerkResult.apiFailure(
        NativeMagicLinkError(reasonCode = NativeMagicLinkReason.MISSING_APPROVAL_TOKEN.code)
      )
    else ->
      ClerkResult.success(ParsedMagicLinkDeepLink(flowId = flowId, approvalToken = approvalToken))
  }
}

internal fun queryOrFragmentParam(uri: Uri, key: String): String? {
  val queryValue = uri.getQueryParameter(key)?.takeIf { it.isNotBlank() }
  val fragmentValue =
    uri.encodedFragment
      ?.split("&")
      .orEmpty()
      .asSequence()
      .mapNotNull { entry ->
        val index = entry.indexOf('=')
        val rawKey = if (index >= 0) entry.substring(0, index) else entry
        val rawValue = if (index >= 0) entry.substring(index + 1) else ""
        val decodedKey = Uri.decode(rawKey)
        if (decodedKey == key) Uri.decode(rawValue) else null
      }
      .firstOrNull()
      ?.takeIf { it.isNotBlank() }

  return queryValue ?: fragmentValue
}

private fun SignIn.emailLinkAddressId(): String? {
  return supportedFirstFactors
    ?.firstOrNull { it.strategy == EMAIL_LINK && it.emailAddressId != null }
    ?.emailAddressId
}

private fun resolveNativeEmailLinkRedirectUri(): String? {
  val applicationId = Clerk.applicationId
  return if (applicationId.isNullOrBlank()) {
    null
  } else {
    RedirectConfiguration.emailLinkRedirectUrl(
      applicationId = applicationId,
      proxyUrl = Clerk.proxyUrl,
    )
  }
}

private fun nativeMagicLinkFailure(
  reason: NativeMagicLinkReason
): ClerkResult.Failure<NativeMagicLinkError> {
  NativeMagicLinkLogger.completeFailure(reason.code)
  return ClerkResult.apiFailure(NativeMagicLinkError(reasonCode = reason.code))
}

private fun currentTimeMillis(): Long = System.currentTimeMillis()

public class NativeMagicLinkError(
  public val reasonCode: String,
  public val message: String? = null,
)

internal enum class NativeMagicLinkReason(val code: String) {
  APPROVAL_TOKEN_CONSUMED("approval_token_consumed"),
  APPROVAL_TOKEN_EXPIRED("approval_token_expired"),
  APPROVAL_TOKEN_INVALID("approval_token_invalid"),
  PKCE_VERIFICATION_FAILED("pkce_verification_failed"),
  FLOW_NOT_APPROVED("flow_not_approved"),
  MISSING_FLOW_ID("missing_flow_id"),
  MISSING_APPROVAL_TOKEN("missing_approval_token"),
  INVALID_IDENTIFIER("invalid_identifier"),
  EMAIL_LINK_NOT_SUPPORTED("email_link_not_supported"),
  NATIVE_API_DISABLED_FOR_INSTANCE("native_api_disabled_for_instance"),
  NATIVE_API_DISABLED("native_api_disabled"),
  NO_PENDING_FLOW("no_pending_flow"),
  FLOW_ID_MISMATCH("native_magic_link_flow_id_mismatch"),
  SESSION_ACTIVATION_FAILED("native_magic_link_session_activation_failed"),
  START_FAILED("native_magic_link_start_failed"),
  PREPARE_FAILED("native_magic_link_prepare_failed"),
  COMPLETE_FAILED("native_magic_link_complete_failed"),
  TICKET_SIGN_IN_FAILED("native_magic_link_ticket_sign_in_failed"),
  TICKET_SIGN_UP_FAILED("native_magic_link_ticket_sign_up_failed"),
}

internal val TERMINAL_REASON_CODES =
  setOf(
    NativeMagicLinkReason.APPROVAL_TOKEN_CONSUMED.code,
    NativeMagicLinkReason.APPROVAL_TOKEN_EXPIRED.code,
    NativeMagicLinkReason.APPROVAL_TOKEN_INVALID.code,
    NativeMagicLinkReason.PKCE_VERIFICATION_FAILED.code,
    NativeMagicLinkReason.FLOW_NOT_APPROVED.code,
  )

internal fun ClerkResult.Failure<ClerkErrorResponse>.toNativeMagicLinkError(
  fallbackReason: NativeMagicLinkReason
): NativeMagicLinkError {
  val firstError = error?.errors?.firstOrNull()
  val backendCode = firstError?.code?.takeIf { it.isNotBlank() }
  val normalizedCode = backendCode ?: fallbackReason.code
  val mapped =
    when (normalizedCode) {
      NativeMagicLinkReason.APPROVAL_TOKEN_CONSUMED.code ->
        NativeMagicLinkReason.APPROVAL_TOKEN_CONSUMED
      NativeMagicLinkReason.APPROVAL_TOKEN_EXPIRED.code ->
        NativeMagicLinkReason.APPROVAL_TOKEN_EXPIRED
      NativeMagicLinkReason.APPROVAL_TOKEN_INVALID.code ->
        NativeMagicLinkReason.APPROVAL_TOKEN_INVALID
      NativeMagicLinkReason.PKCE_VERIFICATION_FAILED.code ->
        NativeMagicLinkReason.PKCE_VERIFICATION_FAILED
      NativeMagicLinkReason.FLOW_NOT_APPROVED.code -> NativeMagicLinkReason.FLOW_NOT_APPROVED
      NativeMagicLinkReason.NATIVE_API_DISABLED_FOR_INSTANCE.code ->
        NativeMagicLinkReason.NATIVE_API_DISABLED_FOR_INSTANCE
      NativeMagicLinkReason.NATIVE_API_DISABLED.code -> NativeMagicLinkReason.NATIVE_API_DISABLED
      else -> null
    }
  val reasonCode = mapped?.code ?: normalizedCode
  return NativeMagicLinkError(
    reasonCode = reasonCode,
    message = firstError?.longMessage ?: firstError?.message,
  )
}

private fun PendingFlowLookup.toCompletionRequestLogDetails(): CompletionRequestLogDetails {
  return when (this) {
    PendingFlowLookup.None ->
      CompletionRequestLogDetails(
        pendingLookupOutcome = "none",
        pendingState = "-",
        expectedFlowId = "-",
      )
    is PendingFlowLookup.Found ->
      CompletionRequestLogDetails(
        pendingLookupOutcome = "found",
        pendingState = flow.state.logName(),
        expectedFlowId = flow.flowId ?: "-",
      )
    is PendingFlowLookup.Mismatched ->
      CompletionRequestLogDetails(
        pendingLookupOutcome = "mismatched",
        pendingState = "-",
        expectedFlowId = expectedFlowId ?: "-",
      )
  }
}

internal object NativeMagicLinkLogger {
  fun start() {
    nativeMagicLinkInfo("native_magic_link_start")
  }

  fun prepareSuccess(state: PendingNativeMagicLinkState, flowId: String) {
    nativeMagicLinkInfo(
      event = "native_magic_link_prepare_success",
      "state" to state.logName(),
      "flow_id" to flowId,
    )
  }

  fun deepLinkReceived(uri: Uri) {
    nativeMagicLinkInfo(
      event = "native_magic_link_deeplink_received",
      "uri_shape" to "{${SafeUriLog.describe(uri)}}",
    )
  }

  fun pendingFlowSaved(flow: PendingNativeMagicLinkFlow) {
    nativeMagicLinkInfo(
      event = "native_magic_link_pending_flow_saved",
      "state" to flow.state.logName(),
      "flow_id" to (flow.flowId ?: "-"),
      "expires_in_ms" to (flow.expiresAtEpochMs - currentTimeMillis()).toString(),
    )
  }

  fun completeRequested(
    flowId: String,
    pendingLookupOutcome: String,
    pendingState: String,
    expectedFlowId: String,
  ) {
    nativeMagicLinkInfo(
      event = "native_magic_link_complete_requested",
      "flow_id" to flowId,
      "pending_lookup" to pendingLookupOutcome,
      "pending_state" to pendingState,
      "expected_flow_id" to expectedFlowId,
    )
  }

  fun completeApiStarted(state: PendingNativeMagicLinkState, hasAttestation: Boolean) {
    nativeMagicLinkInfo(
      event = "native_magic_link_complete_api_started",
      "state" to state.logName(),
      "has_attestation" to hasAttestation.toString(),
    )
  }

  fun ticketReceived(state: PendingNativeMagicLinkState) {
    nativeMagicLinkInfo(event = "native_magic_link_ticket_received", "state" to state.logName())
  }

  fun ticketExchangeSuccess(state: PendingNativeMagicLinkState, createdSessionId: String?) {
    nativeMagicLinkInfo(
      event = "native_magic_link_ticket_exchange_success",
      "state" to state.logName(),
      "has_created_session" to (createdSessionId != null).toString(),
    )
  }

  fun sessionActivationStarted(state: PendingNativeMagicLinkState, createdSessionId: String?) {
    nativeMagicLinkInfo(
      event = "native_magic_link_session_activation_started",
      "state" to state.logName(),
      "has_created_session" to (createdSessionId != null).toString(),
    )
  }

  fun completeSuccess() {
    nativeMagicLinkInfo("native_magic_link_complete_success")
  }

  fun completeFailure(reasonCode: String) {
    nativeMagicLinkWarn(event = "native_magic_link_complete_failure", "reason_code" to reasonCode)
  }
}

private fun PendingNativeMagicLinkState.logName(): String = name.lowercase()

private fun logFlowIdMismatch(expectedFlowId: String?, actualFlowId: String) {
  ClerkLog.w(
    "event=native_magic_link_flow_id_mismatch expected=$expectedFlowId actual=$actualFlowId"
  )
}

private fun nativeMagicLinkInfo(event: String, vararg fields: Pair<String, String>) {
  ClerkLog.i(nativeMagicLinkLogMessage(event, *fields))
}

private fun nativeMagicLinkWarn(event: String, vararg fields: Pair<String, String>) {
  ClerkLog.w(nativeMagicLinkLogMessage(event, *fields))
}

private fun nativeMagicLinkLogMessage(event: String, vararg fields: Pair<String, String>): String {
  return buildString {
    append("event=")
    append(event)
    fields.forEach { (key, value) ->
      append(" ")
      append(key)
      append("=")
      append(value)
    }
    append(" context={")
    append(nativeMagicLinkRuntimeContext())
    append("}")
  }
}

private fun nativeMagicLinkRuntimeContext(): String {
  val baseUrl = runCatching { Clerk.baseUrl }.getOrNull()
  val proxyUrl = Clerk.proxyUrl
  val redirectUri =
    Clerk.applicationId?.let {
      RedirectConfiguration.emailLinkRedirectUrl(applicationId = it, proxyUrl = proxyUrl)
    }

  return buildString {
    append("app_id=")
    append(Clerk.applicationId ?: "-")
    append(", base=")
    append(describeUrlForNativeMagicLink(baseUrl))
    append(", proxy=")
    append(describeUrlForNativeMagicLink(proxyUrl))
    append(", redirect=")
    append(redirectUri ?: "-")
  }
}

private fun describeUrlForNativeMagicLink(value: String?): String {
  return if (value.isNullOrBlank()) {
    "-"
  } else {
    val parsed = runCatching { URL(value) }.getOrNull()
    if (parsed == null) {
      value
    } else {
      val port = parsed.port.takeIf { it > 0 } ?: parsed.defaultPort
      buildString {
        append(parsed.protocol)
        append("://")
        append(parsed.host)
        if (port > 0) {
          append(":")
          append(port)
        }
      }
    }
  }
}
