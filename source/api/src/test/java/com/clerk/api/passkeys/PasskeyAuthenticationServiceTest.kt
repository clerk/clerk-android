package com.clerk.api.passkeys

import android.app.Activity
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.clerk.api.Clerk
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.credentials.shouldSuppressAutomaticCredentialFlowError
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.GoogleSignInService
import com.clerk.api.sso.OAuthResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyAuthenticationServiceTest {

  private lateinit var mockActivity: Activity
  private lateinit var mockCredentialManager: PasskeyCredentialManager
  private lateinit var mockGetCredentialResponse: GetCredentialResponse
  private lateinit var mockSignIn: SignIn
  private lateinit var mockVerification: Verification
  private lateinit var mockPublicKeyCredential: PublicKeyCredential
  private lateinit var mockCustomCredential: CustomCredential
  private lateinit var mockSignInApi: SignInApi
  private lateinit var mockSessionApi: SessionApi

  @Before
  fun setup() {
    mockActivity = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockGetCredentialResponse = mockk(relaxed = true)
    mockSignIn = mockk(relaxed = true)
    mockVerification = mockk(relaxed = true)
    mockPublicKeyCredential = mockk(relaxed = true)
    mockCustomCredential = mockk(relaxed = true)

    mockkObject(Clerk)
    every { Clerk.credentialActivity() } returns mockActivity
    every { Clerk.baseUrl } returns "https://test.clerk.com"

    mockkObject(ClerkApi)
    mockSignInApi = mockk(relaxed = true)
    mockSessionApi = mockk(relaxed = true)
    every { ClerkApi.signIn } returns mockSignInApi
    every { ClerkApi.session } returns mockSessionApi

    GoogleCredentialAuthenticationService.setCredentialManager(mockCredentialManager)
  }

  @After
  fun tearDown() {
    GoogleCredentialAuthenticationService.setCredentialManager(PasskeyCredentialManagerImpl())
    GoogleCredentialAuthenticationService.setGoogleSignInService(GoogleSignInService())
    unmockkAll()
    Clerk.stateStore.reset()
  }

  @Test
  fun `signInWithPasskey succeeds with public key credential`() = runTest {
    val nonce = """{"challenge":"test-challenge","rpId":"passkeys.example.com"}"""
    val authResponseJson = """{"type":"public-key","id":"credential-123"}"""
    val passkeySignIn =
      SignIn(
        id = "sign_in_123",
        status = SignIn.Status.NEEDS_FIRST_FACTOR,
        firstFactorVerification = Verification(nonce = nonce, strategy = "passkey"),
      )
    val completedSignIn = passkeySignIn.copy(status = SignIn.Status.COMPLETE)
    val requestSlot = slot<GetCredentialRequest>()

    every { mockGetCredentialResponse.credential } returns mockPublicKeyCredential
    every { mockPublicKeyCredential.authenticationResponseJson } returns authResponseJson
    coEvery { mockSignInApi.createSignIn(any()) } returns ClerkResult.success(passkeySignIn)
    coEvery { mockCredentialManager.getCredential(any(), capture(requestSlot)) } returns
      mockGetCredentialResponse
    coEvery {
      mockSignInApi.attemptFirstFactor(
        "sign_in_123",
        mapOf("public_key_credential" to authResponseJson, "strategy" to "passkey"),
      )
    } returns ClerkResult.success(completedSignIn)

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY)
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals(completedSignIn, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { mockSignInApi.createSignIn(match { it["strategy"] == "passkey" }) }
    coVerify(exactly = 1) { mockCredentialManager.getCredential(mockActivity, any()) }
    val option = requestSlot.captured.credentialOptions.single() as GetPublicKeyCredentialOption
    val requestJson = Json.parseToJsonElement(option.requestJson).jsonObject
    assertEquals("test-challenge", requestJson.getValue("challenge").jsonPrimitive.content)
    assertEquals("passkeys.example.com", requestJson.getValue("rpId").jsonPrimitive.content)
    assertFalse(requestSlot.captured.preferImmediatelyAvailableCredentials)
    coVerify(exactly = 1) {
      mockSignInApi.attemptFirstFactor(
        "sign_in_123",
        mapOf("public_key_credential" to authResponseJson, "strategy" to "passkey"),
      )
    }
  }

  @Test
  fun `signInWithGoogleCredential signs in with a selected saved password`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val passwordCredential = PasswordCredential("user@example.com", "secret-password")
    val passwordSignIn = SignIn(id = "sign_in_password", status = SignIn.Status.COMPLETE)

    every { mockSignIn.id } returns "sign_in_passkey"
    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    every { mockGetCredentialResponse.credential } returns passwordCredential

    coEvery { mockSignInApi.createSignIn(any()) } answers
      {
        if (firstArg<Map<String, String>>()["strategy"] == "password") {
          ClerkResult.success(passwordSignIn)
        } else {
          ClerkResult.success(mockSignIn)
        }
      }
    coEvery { mockCredentialManager.getCredential(any(), any()) } returns mockGetCredentialResponse

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY, SignIn.CredentialType.PASSWORD)
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals(passwordSignIn, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      mockSignInApi.createSignIn(
        match {
          it["strategy"] == "password" &&
            it["identifier"] == "user@example.com" &&
            it["password"] == "secret-password"
        }
      )
    }
    coVerify(exactly = 0) { mockSignInApi.attemptFirstFactor(any(), any()) }
  }

  @Test
  fun `signInWithGoogleCredential returns password sign in failure`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val passwordCredential = PasswordCredential("user@example.com", "wrong-password")
    val errorResponse =
      ClerkErrorResponse(
        errors =
          listOf(
            Error(
              code = "form_password_incorrect",
              message = "Incorrect password",
              longMessage = "Password is incorrect.",
            )
          ),
        clerkTraceId = "test-trace",
      )

    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    every { mockGetCredentialResponse.credential } returns passwordCredential

    coEvery { mockSignInApi.createSignIn(any()) } answers
      {
        if (firstArg<Map<String, String>>()["strategy"] == "password") {
          ClerkResult.apiFailure(errorResponse)
        } else {
          ClerkResult.success(mockSignIn)
        }
      }
    coEvery { mockCredentialManager.getCredential(any(), any()) } returns mockGetCredentialResponse

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSWORD)
      )

    assertTrue(result is ClerkResult.Failure)
    assertEquals(errorResponse, (result as ClerkResult.Failure).error)
    coVerify(exactly = 0) { mockSignInApi.attemptFirstFactor(any(), any()) }
  }

  @Test
  fun `signInWithGoogleCredential returns completed Google sign up as completed sign in`() =
    runTest {
      val signUp = mockk<SignUp>(relaxed = true)
      every { signUp.id } returns "sign_up_123"
      every { signUp.status } returns SignUp.Status.COMPLETE
      every { signUp.emailAddress } returns "new-user@example.com"
      every { signUp.createdSessionId } returns "sess_new"
      stubGoogleCredentialResult(ClerkResult.success(OAuthResult(signUp = signUp)))

      val result =
        GoogleCredentialAuthenticationService.signInWithGoogleCredential(
          credentialTypes = listOf(SignIn.CredentialType.GOOGLE)
        )

      assertTrue(result is ClerkResult.Success)
      val signIn = (result as ClerkResult.Success).value
      assertEquals("sign_up_123", signIn.id)
      assertEquals(SignIn.Status.COMPLETE, signIn.status)
      assertEquals("sess_new", signIn.createdSessionId)
      assertEquals("new-user@example.com", signIn.identifier)
    }

  @Test
  fun `signInWithGoogleCredential fails clearly for incomplete Google sign up`() = runTest {
    val signUp = mockk<SignUp>(relaxed = true)
    every { signUp.status } returns SignUp.Status.MISSING_REQUIREMENTS
    stubGoogleCredentialResult(ClerkResult.success(OAuthResult(signUp = signUp)))

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.GOOGLE)
      )

    assertTrue(result is ClerkResult.Failure)
    assertTrue(
      (result as ClerkResult.Failure).throwable is CredentialFlowException.SignUpIncomplete
    )
  }

  @Test
  fun `signInWithPasskey can prefer immediately available credentials`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val authResponseJson = """{"type":"public-key","id":"credential-123"}"""
    val requestSlot = slot<GetCredentialRequest>()

    every { mockSignIn.id } returns "sign_in_123"
    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    every { mockGetCredentialResponse.credential } returns mockPublicKeyCredential
    every { mockPublicKeyCredential.authenticationResponseJson } returns authResponseJson

    coEvery { mockSignInApi.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockSignInApi.attemptFirstFactor(any(), any()) } returns
      ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), capture(requestSlot)) } returns
      mockGetCredentialResponse

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        preferImmediatelyAvailableCredentials = true,
      )

    assertTrue(result is ClerkResult.Success)
    assertTrue(requestSlot.captured.preferImmediatelyAvailableCredentials)
  }

  private fun stubGoogleCredentialResult(result: ClerkResult<OAuthResult, ClerkErrorResponse>) {
    val googleCredential = mockk<CustomCredential>(relaxed = true)
    val googleSignInService = mockk<GoogleSignInService>()
    val environment = mockk<Environment>(relaxed = true)
    every { environment.displayConfig.googleOneTapClientId } returns "google-client-id"
    every { Clerk.environment } returns environment
    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns """{"challenge":"test-challenge"}"""
    every { mockGetCredentialResponse.credential } returns googleCredential
    coEvery { mockSignInApi.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } returns mockGetCredentialResponse
    coEvery { googleSignInService.handleSignInResult(googleCredential, any()) } returns result
    GoogleCredentialAuthenticationService.setGoogleSignInService(googleSignInService)
  }

  @Test
  fun `authenticateWithPasskey uses second factor endpoints for an in-progress sign in`() =
    runTest {
      val nonce = """{"challenge":"test-challenge"}"""
      val authResponseJson = """{"type":"public-key","id":"credential-123"}"""
      val signIn =
        SignIn(
          id = "sign_in_123",
          status = SignIn.Status.NEEDS_SECOND_FACTOR,
          supportedSecondFactors = listOf(Factor(strategy = "passkey")),
        )
      val preparedSignIn =
        signIn.copy(secondFactorVerification = Verification(nonce = nonce, strategy = "passkey"))
      val completedSignIn = preparedSignIn.copy(status = SignIn.Status.COMPLETE)

      every { mockGetCredentialResponse.credential } returns mockPublicKeyCredential
      every { mockPublicKeyCredential.authenticationResponseJson } returns authResponseJson
      coEvery { mockCredentialManager.getCredential(any(), any()) } returns
        mockGetCredentialResponse
      coEvery {
        mockSignInApi.prepareSecondFactor("sign_in_123", mapOf("strategy" to "passkey"))
      } returns ClerkResult.success(preparedSignIn)
      coEvery {
        mockSignInApi.attemptSecondFactor(
          "sign_in_123",
          mapOf("public_key_credential" to authResponseJson, "strategy" to "passkey"),
        )
      } returns ClerkResult.success(completedSignIn)

      val result = GoogleCredentialAuthenticationService.authenticateWithPasskey(signIn)

      assertTrue(result is ClerkResult.Success)
      assertEquals(completedSignIn, (result as ClerkResult.Success).value)
      coVerify(exactly = 0) { mockSignInApi.createSignIn(any()) }
      coVerify(exactly = 1) {
        mockSignInApi.prepareSecondFactor("sign_in_123", mapOf("strategy" to "passkey"))
      }
      coVerify(exactly = 1) {
        mockSignInApi.attemptSecondFactor(
          "sign_in_123",
          mapOf("public_key_credential" to authResponseJson, "strategy" to "passkey"),
        )
      }
    }

  @Test
  fun `signInWithPasskey returns error when SignIn creation fails`() = runTest {
    val error =
      Error(
        code = "signin_error",
        message = "Failed to create signin",
        longMessage = "SignIn creation failed",
      )
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test-trace")

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.apiFailure(errorResponse)

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        listOf(SignIn.CredentialType.PASSKEY)
      )

    assertTrue(result is ClerkResult.Failure)
    assertEquals(errorResponse, (result as ClerkResult.Failure).error)
    coVerify(exactly = 0) { mockCredentialManager.getCredential(any(), any()) }
  }

  @Test
  fun `signInWithPasskey rethrows coroutine cancellation instead of returning a failure`() =
    runTest {
      every { mockSignIn.firstFactorVerification } returns mockVerification
      every { mockVerification.nonce } returns """{"challenge":"test-challenge"}"""
      coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
      coEvery { mockCredentialManager.getCredential(any(), any()) } throws
        CancellationException("caller cancelled")

      val thrown = runCatching {
        GoogleCredentialAuthenticationService.signInWithGoogleCredential(
          listOf(SignIn.CredentialType.PASSKEY)
        )
      }
        .exceptionOrNull()

      assertTrue(thrown is CancellationException)
    }

  @Test
  fun `authenticateWithPasskey second factor rethrows coroutine cancellation`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val signIn =
      SignIn(
        id = "sign_in_123",
        status = SignIn.Status.NEEDS_SECOND_FACTOR,
        supportedSecondFactors = listOf(Factor(strategy = "passkey")),
      )
    coEvery {
      mockSignInApi.prepareSecondFactor("sign_in_123", mapOf("strategy" to "passkey"))
    } returns
      ClerkResult.success(
        signIn.copy(secondFactorVerification = Verification(nonce = nonce, strategy = "passkey"))
      )
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws
      CancellationException("caller cancelled")

    val thrown = runCatching {
      GoogleCredentialAuthenticationService.authenticateWithPasskey(signIn)
    }
      .exceptionOrNull()

    assertTrue(thrown is CancellationException)
  }

  @Test
  fun `verifySessionWithPasskey rethrows coroutine cancellation`() = runTest {
    val preparedVerification =
      SessionVerification(
        id = "ver_123",
        status = SessionVerification.Status.NEEDS_FIRST_FACTOR,
        level = SessionVerification.Level.FIRST_FACTOR,
        firstFactorVerification =
          Verification(nonce = """{"challenge":"test-challenge"}""", strategy = "passkey"),
      )
    coEvery { mockSessionApi.prepareFirstFactorVerification("sess_123", any()) } returns
      ClerkResult.success(preparedVerification)
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws
      CancellationException("caller cancelled")

    val thrown = runCatching {
      GoogleCredentialAuthenticationService.verifySessionWithPasskey(testSession())
    }
      .exceptionOrNull()

    assertTrue(thrown is CancellationException)
  }

  @Test
  fun `signInWithPasskey handles NoCredentialException`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val exception = NoCredentialException("No credentials available")

    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws exception

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        listOf(SignIn.CredentialType.PASSKEY)
      )

    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertTrue(failure.throwable is CredentialFlowException.NoSavedCredential)
  }

  @Test
  fun `automatic signInWithPasskey clears suppressed current sign in`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val client = Client(id = "client_123", signIn = mockSignIn)

    every { mockSignIn.id } returns "sign_in_123"
    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    Clerk.updateClient(client)

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws
      NoCredentialException("No credentials available")

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        preferImmediatelyAvailableCredentials = true,
      )

    assertTrue(result is ClerkResult.Failure)
    assertTrue(
      (result as ClerkResult.Failure).throwable is CredentialFlowException.NoSavedCredential
    )
    assertEquals(client.copy(signIn = null), Clerk.client)
  }

  @Test
  fun `automatic signInWithPasskey clears sign in on ProviderUnavailable`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val client = Client(id = "client_123", signIn = mockSignIn)

    every { mockSignIn.id } returns "sign_in_123"
    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    Clerk.updateClient(client)

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws
      GetCredentialProviderConfigurationException("Provider not configured")

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        preferImmediatelyAvailableCredentials = true,
      )

    assertTrue(result is ClerkResult.Failure)
    assertTrue(
      (result as ClerkResult.Failure).throwable is CredentialFlowException.ProviderUnavailable
    )
    assertEquals(client.copy(signIn = null), Clerk.client)
  }

  @Test
  fun `automatic signInWithPasskey clears sign in on MissingActivity`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val client = Client(id = "client_123", signIn = mockSignIn)

    every { mockSignIn.id } returns "sign_in_123"
    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    Clerk.updateClient(client)

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws
      GetCredentialUnknownException("Activity-based context is required")

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        preferImmediatelyAvailableCredentials = true,
      )

    assertTrue(result is ClerkResult.Failure)
    assertTrue((result as ClerkResult.Failure).throwable is CredentialFlowException.MissingActivity)
    assertEquals(client.copy(signIn = null), Clerk.client)
  }

  @Test
  fun `automatic signInWithPasskey clears sign in on GetCredentialUnknownException`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val client = Client(id = "client_123", signIn = mockSignIn)

    every { mockSignIn.id } returns "sign_in_123"
    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    Clerk.updateClient(client)

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws
      GetCredentialUnknownException("asset links mismatch")

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        preferImmediatelyAvailableCredentials = true,
      )

    assertTrue(result is ClerkResult.Failure)
    assertTrue((result as ClerkResult.Failure).throwable is GetCredentialUnknownException)
    assertEquals(client.copy(signIn = null), Clerk.client)
  }

  @Test
  fun `signInWithPasskey handles cancellation without generic failure`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""

    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } throws
      GetCredentialCancellationException()

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        listOf(SignIn.CredentialType.PASSKEY)
      )

    assertTrue(result is ClerkResult.Failure)
    assertTrue((result as ClerkResult.Failure).throwable is CredentialFlowException.UserCancelled)
  }

  @Test
  fun `automatic credential flow suppresses user cancellation`() {
    val result = ClerkResult.unknownFailure(CredentialFlowException.UserCancelled())

    assertTrue(result.shouldSuppressAutomaticCredentialFlowError)
  }

  @Test
  fun `automatic credential flow suppresses no saved credential`() {
    val result = ClerkResult.unknownFailure(CredentialFlowException.NoSavedCredential())

    assertTrue(result.shouldSuppressAutomaticCredentialFlowError)
  }

  @Test
  fun `automatic credential flow suppresses provider unavailable`() {
    val result = ClerkResult.unknownFailure(CredentialFlowException.ProviderUnavailable())

    assertTrue(result.shouldSuppressAutomaticCredentialFlowError)
  }

  @Test
  fun `automatic credential flow suppresses missing activity`() {
    val result = ClerkResult.unknownFailure(CredentialFlowException.MissingActivity())

    assertTrue(result.shouldSuppressAutomaticCredentialFlowError)
  }

  @Test
  fun `automatic credential flow suppresses unclassified credential ceremony failures`() {
    val result = ClerkResult.unknownFailure(GetCredentialUnknownException("asset links mismatch"))

    assertTrue(result.shouldSuppressAutomaticCredentialFlowError)
  }

  @Test
  fun `automatic credential flow does not suppress non-credential failures`() {
    val result = ClerkResult.unknownFailure(IllegalStateException("serialization failed"))

    assertFalse(result.shouldSuppressAutomaticCredentialFlowError)
  }

  @Test
  fun `signInWithPasskey fails when no activity is available`() = runTest {
    every { Clerk.credentialActivity() } returns null

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        listOf(SignIn.CredentialType.PASSKEY)
      )

    assertTrue(result is ClerkResult.Failure)
    assertTrue((result as ClerkResult.Failure).throwable is CredentialFlowException.MissingActivity)
    coVerify(exactly = 0) { mockCredentialManager.getCredential(any(), any()) }
  }

  @Test
  fun `signInWithPasskey returns error for unknown credential type`() = runTest {
    val nonce = """{"challenge":"test-challenge"}"""
    val unknownCredential = mockk<Credential>(relaxed = true)

    every { mockSignIn.firstFactorVerification } returns mockVerification
    every { mockVerification.nonce } returns nonce
    every { mockGetCredentialResponse.credential } returns unknownCredential

    coEvery { ClerkApi.signIn.createSignIn(any()) } returns ClerkResult.success(mockSignIn)
    coEvery { mockCredentialManager.getCredential(any(), any()) } returns mockGetCredentialResponse

    val result =
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        listOf(SignIn.CredentialType.PASSKEY)
      )

    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertTrue(failure.throwable is IllegalStateException)
    assertEquals("Unknown credential type", failure.throwable?.message)
    coVerify(exactly = 0) { mockSignInApi.attemptFirstFactor(any(), any()) }
  }

  @Test
  fun `verifySessionWithPasskey uses second factor endpoints when requested`() = runTest {
    val session = testSession()
    val nonce = """{"challenge":"test-challenge"}"""
    val authResponseJson = """{"type":"public-key","id":"credential-123"}"""
    val preparedVerification =
      SessionVerification(
        id = "ver_123",
        status = SessionVerification.Status.NEEDS_SECOND_FACTOR,
        level = SessionVerification.Level.MULTI_FACTOR,
        secondFactorVerification = Verification(nonce = nonce, strategy = "passkey"),
      )
    val completedVerification =
      preparedVerification.copy(status = SessionVerification.Status.COMPLETE)

    every { mockGetCredentialResponse.credential } returns mockPublicKeyCredential
    every { mockPublicKeyCredential.authenticationResponseJson } returns authResponseJson
    coEvery { mockCredentialManager.getCredential(any(), any()) } returns mockGetCredentialResponse
    coEvery {
      mockSessionApi.prepareSecondFactorVerification("sess_123", mapOf("strategy" to "passkey"))
    } returns ClerkResult.success(preparedVerification)
    coEvery {
      mockSessionApi.attemptSecondFactorVerification(
        "sess_123",
        mapOf("public_key_credential" to authResponseJson, "strategy" to "passkey"),
      )
    } returns ClerkResult.success(completedVerification)

    val result =
      GoogleCredentialAuthenticationService.verifySessionWithPasskey(
        session = session,
        level = SessionVerification.Level.SECOND_FACTOR,
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals(completedVerification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      mockSessionApi.prepareSecondFactorVerification("sess_123", mapOf("strategy" to "passkey"))
    }
    coVerify(exactly = 1) {
      mockSessionApi.attemptSecondFactorVerification(
        "sess_123",
        mapOf("public_key_credential" to authResponseJson, "strategy" to "passkey"),
      )
    }
  }

  private fun testSession(): Session {
    return Session(
      id = "sess_123",
      status = Session.SessionStatus.ACTIVE,
      expireAt = 0L,
      lastActiveAt = 0L,
      createdAt = 0L,
      updatedAt = 0L,
    )
  }
}
