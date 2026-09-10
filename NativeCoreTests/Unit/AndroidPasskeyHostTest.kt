package com.clerk.api

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import androidx.credentials.*
import androidx.credentials.exceptions.*
import java.util.concurrent.Executor
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/** Runs the production host and AndroidX manager; only provider discovery is substituted. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [28, 36],
  manifest = Config.NONE,
  instrumentedPackages = ["androidx.credentials"],
  shadows = [PasskeyProviderFactoryShadow::class],
)
class AndroidPasskeyHostTest {
  private lateinit var activityController: ActivityController<Activity>
  private lateinit var activity: Activity
  private lateinit var provider: PasskeyProviderFixture

  @Before
  fun setup() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    activityController = Robolectric.buildActivity(Activity::class.java).setup()
    activity = activityController.get()
    provider = PasskeyProviderFixture()
    PasskeyProviderFactoryShadow.provider = provider
  }

  @After
  fun close() {
    PasskeyProviderFactoryShadow.provider = null
    if (!activity.isDestroyed) activityController.pause().stop().destroy()
    Dispatchers.resetMain()
  }

  private fun host(presenter: (() -> Activity?)? = { activity }) =
    AndroidCapabilities(
      publishableKey = "fixture",
      frontendAPI = "https://fixture.invalid",
      activity = presenter,
      storage =
        object : CredentialStorage {
          override suspend fun read(): String? = error("Unexpected storage")

          override suspend fun write(value: String) {
            error("Unexpected storage")
          }

          override suspend fun remove() {
            error("Unexpected storage")
          }
        },
    )

  private val getOptions =
    Json.parseToJsonElement(
      """{
    "challenge":{"base64url":"AQID"},"rpId":"passkeys.example.com","timeout":60000,
    "userVerification":"required",
    "allowCredentials":[{"type":"public-key","id":{"base64url":"BAUG"},"transports":["internal","hybrid"]},{"type":"public-key","id":{"base64url":"BwgJ"}}],
    "conditionalUI":false,"preferImmediatelyAvailableCredentials":true
  }"""
    )
  private val createOptions =
    Json.parseToJsonElement(
      """{
    "challenge":{"base64url":"AQID"},"rp":{"id":"passkeys.example.com","name":"Example"},
    "user":{"id":{"base64url":"BAUG"},"name":"person@example.com","displayName":"Person"},
    "pubKeyCredParams":[{"type":"public-key","alg":-7}],
    "excludeCredentials":[{"type":"public-key","id":{"base64url":"BwgJ"},"transports":["internal"]}],
    "authenticatorSelection":{"userVerification":"required","residentKey":"required"}
  }"""
    )
  private val assertion =
    Json.parseToJsonElement(
      """{
    "type":"public-key","id":"BAUG","rawId":"BAUG","authenticatorAttachment":"platform",
    "response":{"clientDataJSON":"e30","authenticatorData":"AQID","signature":"BwgJ","userHandle":null}
  }"""
    )
  private val registration =
    Json.parseToJsonElement(
      """{
    "type":"public-key","id":"BAUG","rawId":"BAUG","authenticatorAttachment":"platform",
    "response":{"clientDataJSON":"e30","attestationObject":"AQID","transports":["internal"]}
  }"""
    )

  @Test
  fun requestsPreserveWebAuthnOptionsAndReturnPublicKeyResponses() = runTest {
    provider.create = { request, callback ->
      check(request is CreatePublicKeyCredentialRequest)
      check(
        Json.parseToJsonElement(request.requestJson) ==
          Json.parseToJsonElement(
            """{
        "challenge":"AQID","rp":{"id":"passkeys.example.com","name":"Example"},
        "user":{"id":"BAUG","name":"person@example.com","displayName":"Person"},
        "pubKeyCredParams":[{"type":"public-key","alg":-7}],
        "excludeCredentials":[{"type":"public-key","id":"BwgJ","transports":["internal"]}],
        "authenticatorSelection":{"userVerification":"required","residentKey":"required"}
      }"""
          )
      )
      callback.onResult(CreatePublicKeyCredentialResponse(registration.toString()))
    }
    provider.get = { request, callback ->
      check(request.preferImmediatelyAvailableCredentials)
      val option = request.credentialOptions.single() as GetPublicKeyCredentialOption
      check(
        Json.parseToJsonElement(option.requestJson) ==
          Json.parseToJsonElement(
            """{
        "challenge":"AQID","rpId":"passkeys.example.com","timeout":60000,"userVerification":"required",
        "allowCredentials":[{"type":"public-key","id":"BAUG","transports":["internal","hybrid"]},{"type":"public-key","id":"BwgJ"}]
      }"""
          )
      )
      callback.onResult(GetCredentialResponse(PublicKeyCredential(assertion.toString())))
    }
    val host = host()
    check(host.perform("passkeys.create", createOptions) == registration)
    check(host.perform("passkeys.get", getOptions) == assertion)
    check(provider.calls == listOf("create", "get"))
    check(provider.contexts.all { it === activity })
    check(provider.signals.all { !it.isCanceled })
  }

  @Test
  fun credentialTypeMismatchFailsWithoutRetrying() = runTest {
    provider.create = { _, callback -> callback.onResult(CreatePasswordResponse()) }
    provider.get = { _, callback ->
      callback.onResult(GetCredentialResponse(PasswordCredential("id", "password")))
    }
    for ((operation, options) in listOf("create" to createOptions, "get" to getOptions)) {
      val failure = runCatching { host().perform("passkeys.$operation", options) }.exceptionOrNull()
      check(failure is CoreException && failure.code == "invalid_credential_response")
    }
    check(provider.calls == listOf("create", "get"))
  }

  @Test
  fun platformCancellationIsReportedAsUserCancelled() = runTest {
    provider.create = { _, callback -> callback.onError(CreateCredentialCancellationException()) }
    provider.get = { _, callback -> callback.onError(GetCredentialCancellationException()) }
    for ((operation, options) in listOf("create" to createOptions, "get" to getOptions)) {
      val failure = runCatching { host().perform("passkeys.$operation", options) }.exceptionOrNull()
      check(failure is CoreException && failure.code == "user_cancelled")
      currentCoroutineContext().ensureActive()
    }
    check(provider.calls == listOf("create", "get"))
  }

  @Test
  fun providerFailuresPropagateWithoutRetry() = runTest {
    val createFailure =
      CreateCredentialProviderConfigurationException("Fixture provider unavailable")
    val getFailure = NoCredentialException("Fixture has no credential")
    provider.create = { _, callback -> callback.onError(createFailure) }
    provider.get = { _, callback -> callback.onError(getFailure) }
    check(
      runCatching { host().perform("passkeys.create", createOptions) }.exceptionOrNull() ===
        createFailure
    )
    check(
      runCatching { host().perform("passkeys.get", getOptions) }.exceptionOrNull() === getFailure
    )
    check(provider.calls == listOf("create", "get"))
  }

  @Test
  fun callerCancellationCancelsSystemRequestAndIgnoresLateReply() = runTest {
    val host = host()
    for ((operation, options) in listOf("create" to createOptions, "get" to getOptions)) {
      lateinit var lateReply: () -> Unit
      provider.create = { _, callback ->
        lateReply = {
          callback.onResult(CreatePublicKeyCredentialResponse(registration.toString()))
        }
      }
      provider.get = { _, callback ->
        lateReply = {
          callback.onResult(GetCredentialResponse(PublicKeyCredential(assertion.toString())))
        }
      }
      val request =
        async(start = CoroutineStart.UNDISPATCHED) { host.perform("passkeys.$operation", options) }
      val signal = provider.signals.last()
      check(!request.isCompleted && !signal.isCanceled)
      request.cancelAndJoin()
      check(signal.isCanceled)
      lateReply()
      check(runCatching { request.await() }.exceptionOrNull() is CancellationException)
    }
    provider.get = { _, callback ->
      callback.onResult(GetCredentialResponse(PublicKeyCredential(assertion.toString())))
    }
    check(host.perform("passkeys.get", getOptions) == assertion)
    check(!provider.signals.last().isCanceled)
    check(provider.signals.distinct().size == 3)
    check(provider.calls == listOf("create", "get", "get"))
  }

  @Test
  fun missingAndFinishedActivitiesFailBeforeCredentialRequest() = runTest {
    suspend fun verify(host: AndroidCapabilities) {
      for ((operation, options) in listOf("create" to createOptions, "get" to getOptions)) {
        val failure = runCatching { host.perform("passkeys.$operation", options) }.exceptionOrNull()
        check(failure is CoreException && failure.code == "presentation_unavailable")
      }
    }
    check("passkeys" !in host(null).supported)
    verify(host(null))
    verify(host { null })
    activity.finish()
    verify(host())
    activityController.pause().stop().destroy()
    verify(host())
    check(provider.calls.isEmpty())
  }

  @Test
  fun unsupportedOptionsFailBeforeCredentialRequest() = runTest {
    val autofill = JsonObject(getOptions.jsonObject + ("conditionalUI" to JsonPrimitive(true)))
    val failure = runCatching { host().perform("passkeys.get", autofill) }.exceptionOrNull()
    check(failure is CoreException && failure.code == "capability_unavailable")
    for (invalid in listOf("", "!!!")) {
      val options =
        JsonObject(
          getOptions.jsonObject + ("challenge" to buildJsonObject { put("base64url", invalid) })
        )
      val error = runCatching { host().perform("passkeys.get", options) }.exceptionOrNull()
      check(error is CoreException && error.code == "invalid_credential_options")
    }
    check(provider.calls.isEmpty())
  }
}

@Implements(className = "androidx.credentials.CredentialProviderFactory", isInAndroidSdk = false)
class PasskeyProviderFactoryShadow {
  companion object {
    var provider: CredentialProvider? = null
  }

  @Implementation
  fun getBestAvailableProvider(request: Any, shouldFallback: Boolean): CredentialProvider =
    checkNotNull(provider)
}

class PasskeyProviderFixture : CredentialProvider {
  val calls = mutableListOf<String>()
  val contexts = mutableListOf<Context>()
  val signals = mutableListOf<CancellationSignal>()
  var create:
    (
      CreateCredentialRequest,
      CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
    ) -> Unit =
    { _, _ ->
      error("Unexpected create")
    }
  var get:
    (
      GetCredentialRequest,
      CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
    ) -> Unit =
    { _, _ ->
      error("Unexpected get")
    }

  override fun isAvailableOnDevice() = true

  override fun onCreateCredential(
    context: Context,
    request: CreateCredentialRequest,
    cancellationSignal: CancellationSignal?,
    executor: Executor,
    callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
  ) {
    calls += "create"
    contexts += context
    signals += checkNotNull(cancellationSignal)
    executor.execute { create(request, callback) }
  }

  override fun onGetCredential(
    context: Context,
    request: GetCredentialRequest,
    cancellationSignal: CancellationSignal?,
    executor: Executor,
    callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
  ) {
    calls += "get"
    contexts += context
    signals += checkNotNull(cancellationSignal)
    executor.execute { get(request, callback) }
  }

  override fun onClearCredential(
    request: ClearCredentialStateRequest,
    cancellationSignal: CancellationSignal?,
    executor: Executor,
    callback: CredentialManagerCallback<Void?, ClearCredentialException>,
  ) {
    error("Unexpected clear")
  }
}
