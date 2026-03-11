package com.clerk.api.magiclink

import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.api.MagicLinkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.magiclink.NativeMagicLinkCompleteResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.storage.StorageHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NativeMagicLinkServiceTest {
  private lateinit var signInApi: SignInApi
  private lateinit var signUpApi: SignUpApi
  private lateinit var magicLinkApi: MagicLinkApi
  private lateinit var clientApi: ClientApi
  private lateinit var auth: Auth

  @Before
  fun setup() {
    StorageHelper.initialize(RuntimeEnvironment.getApplication())
    StorageHelper.reset(RuntimeEnvironment.getApplication())

    signInApi = mockk(relaxed = true)
    signUpApi = mockk(relaxed = true)
    magicLinkApi = mockk(relaxed = true)
    clientApi = mockk(relaxed = true)
    auth = mockk(relaxed = true)

    mockkObject(ClerkApi)
    mockkObject(Clerk)

    every { ClerkApi.signIn } returns signInApi
    every { ClerkApi.signUp } returns signUpApi
    every { ClerkApi.magicLink } returns magicLinkApi
    every { ClerkApi.client } returns clientApi
    every { Clerk.auth } returns auth
    every { Clerk.locale } returns MutableStateFlow("en")
    every { Clerk.applicationId } returns "com.clerk.test"
    every { Clerk.proxyUrl } returns null
    every { Clerk.updateClient(any()) } just runs

    NativeMagicLinkService.resetForTests()
  }

  @After
  fun tearDown() {
    NativeMagicLinkService.resetForTests()
    StorageHelper.reset(RuntimeEnvironment.getApplication())
    unmockkAll()
  }

  @Test
  fun `end-to-end native magic link flow completes with ticket sign-in`() = runTest {
    val initialSignIn =
      SignIn(
        id = "sign_in_123",
        supportedFirstFactors =
          listOf(Factor(strategy = "email_link", emailAddressId = "email_123")),
      )
    val preparedSignIn = initialSignIn.copy(status = SignIn.Status.NEEDS_FIRST_FACTOR)
    val completedSignIn =
      initialSignIn.copy(status = SignIn.Status.COMPLETE, createdSessionId = "sess_123")
    val refreshedClient = mockk<Client>(relaxed = true)
    val activatedSession = mockk<Session>(relaxed = true)

    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(initialSignIn)
    coEvery { signInApi.prepareSignInFirstFactor(any(), any()) } returns
      ClerkResult.success(preparedSignIn)
    coEvery { magicLinkApi.complete(any()) } returns
      ClerkResult.success(NativeMagicLinkCompleteResponse(ticket = "ticket_123"))
    coEvery { auth.signInWithTicket("ticket_123") } returns ClerkResult.success(completedSignIn)
    coEvery { auth.setActive("sess_123", null) } returns ClerkResult.success(activatedSession)
    coEvery { clientApi.get() } returns ClerkResult.success(refreshedClient)

    val startResult = NativeMagicLinkService.startEmailLinkSignIn("user@example.com")
    assertTrue(startResult is ClerkResult.Success)

    val callbackUri =
      Uri.parse("clerk://com.clerk.test.oauth?flow_id=flow_123&approval_token=approval_123")
    val completeResult = NativeMagicLinkService.handleMagicLinkDeepLink(callbackUri)
    assertTrue(completeResult is ClerkResult.Success)
    assertEquals(SignIn.Status.COMPLETE, (completeResult as ClerkResult.Success).value.status)

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        "sign_in_123",
        match {
          it["strategy"] == "email_link" &&
            it["email_address_id"] == "email_123" &&
            it["code_challenge_method"] == "S256" &&
            !it.containsKey("native_flow") &&
            !it.containsKey("code_verifier")
        },
      )
    }
    coVerify(exactly = 1) {
      magicLinkApi.complete(
        match {
          it["flow_id"] == "flow_123" &&
            it["approval_token"] == "approval_123" &&
            it["code_verifier"]?.isNotBlank() == true
        }
      )
    }
    coVerify(exactly = 1) { auth.signInWithTicket("ticket_123") }
    coVerify(exactly = 1) { auth.setActive("sess_123", null) }
    coVerify(exactly = 0) { signInApi.fetchSignIn(any(), any()) }
  }

  @Test
  fun `sign-up native email-link prepare stores PKCE verifier for callback completion`() = runTest {
    val preparedSignUp = mockk<SignUp>(relaxed = true)
    val completedSignIn =
      SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE, createdSessionId = "sess_456")
    val refreshedClient = mockk<Client>(relaxed = true)
    val activatedSession = mockk<Session>(relaxed = true)
    val strategy =
      SignUp.PrepareVerificationParams.Strategy.EmailLink(
        redirectUri = "clerk://com.clerk.test.oauth"
      )

    coEvery { signUpApi.prepareSignUpVerification("sign_up_123", any()) } returns
      ClerkResult.success(preparedSignUp)
    coEvery { magicLinkApi.complete(any()) } returns
      ClerkResult.success(NativeMagicLinkCompleteResponse(ticket = "ticket_signup"))
    coEvery { auth.signInWithTicket("ticket_signup") } returns ClerkResult.success(completedSignIn)
    coEvery { auth.setActive("sess_456", null) } returns ClerkResult.success(activatedSession)
    coEvery { clientApi.get() } returns ClerkResult.success(refreshedClient)

    val prepareResult =
      NativeMagicLinkService.prepareSignUpEmailLink(signUpId = "sign_up_123", strategy = strategy)
    assertTrue(prepareResult is ClerkResult.Success)

    val callbackUri =
      Uri.parse("clerk://com.clerk.test.oauth?flow_id=sua_123&approval_token=approval_123")
    val completeResult = NativeMagicLinkService.handleMagicLinkDeepLink(callbackUri)
    assertTrue(completeResult is ClerkResult.Success)

    coVerify(exactly = 1) {
      signUpApi.prepareSignUpVerification(
        "sign_up_123",
        match {
          it["strategy"] == "email_link" &&
            it["redirect_uri"] == "clerk://com.clerk.test.oauth" &&
            it["code_challenge_method"] == "S256" &&
            it["code_challenge"]?.isNotBlank() == true
        },
      )
    }
    coVerify(exactly = 1) {
      magicLinkApi.complete(
        match {
          it["flow_id"] == "sua_123" &&
            it["approval_token"] == "approval_123" &&
            it["code_verifier"]?.isNotBlank() == true
        }
      )
    }
    coVerify(exactly = 1) { auth.setActive("sess_456", null) }
  }

  @Test
  fun `complete returns activation failure when created session cannot be activated`() = runTest {
    val initialSignIn =
      SignIn(
        id = "sign_in_123",
        supportedFirstFactors =
          listOf(Factor(strategy = "email_link", emailAddressId = "email_123")),
      )
    val preparedSignIn = initialSignIn.copy(status = SignIn.Status.NEEDS_FIRST_FACTOR)
    val completedSignIn =
      initialSignIn.copy(status = SignIn.Status.COMPLETE, createdSessionId = "sess_unactivated")
    val refreshedClient = mockk<Client>(relaxed = true)
    val apiError =
      ClerkErrorResponse(
        errors =
          listOf(
            Error(
              message = "is invalid",
              longMessage = "Could not activate session",
              code = "session_cannot_be_activated",
            )
          )
      )

    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(initialSignIn)
    coEvery { signInApi.prepareSignInFirstFactor(any(), any()) } returns
      ClerkResult.success(preparedSignIn)
    coEvery { magicLinkApi.complete(any()) } returns
      ClerkResult.success(NativeMagicLinkCompleteResponse(ticket = "ticket_123"))
    coEvery { auth.signInWithTicket("ticket_123") } returns ClerkResult.success(completedSignIn)
    coEvery { auth.setActive("sess_unactivated", null) } returns ClerkResult.apiFailure(apiError)
    coEvery { clientApi.get() } returns ClerkResult.success(refreshedClient)

    val startResult = NativeMagicLinkService.startEmailLinkSignIn("user@example.com")
    assertTrue(startResult is ClerkResult.Success)

    val callbackUri =
      Uri.parse("clerk://com.clerk.test.oauth?flow_id=flow_123&approval_token=approval_123")
    val completeResult = NativeMagicLinkService.handleMagicLinkDeepLink(callbackUri)

    assertTrue(completeResult is ClerkResult.Failure)
    assertEquals(
      "session_cannot_be_activated",
      (completeResult as ClerkResult.Failure).error?.reasonCode,
    )
    assertEquals("Could not activate session", completeResult.error?.message)
    coVerify(exactly = 1) { auth.setActive("sess_unactivated", null) }
  }
}
