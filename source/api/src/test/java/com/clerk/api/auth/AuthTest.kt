package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.api.SET_ACTIVE_INTENT_SELECT_ORG
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.OrganizationSettings
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOService
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthTest {

  @Before
  fun setup() {
    Clerk.updateClient(Client())
    setForceOrganizationSelection(false)
  }

  @After
  fun tearDown() {
    unmockkAll()
    Clerk.updateClient(Client())
    setForceOrganizationSelection(false)
  }

  @Test
  fun `signUpWithGoogleOneTap delegates to transferable Google One Tap flow`() = runTest {
    mockkObject(SignIn.Companion)
    val signUp = mockk<SignUp>(relaxed = true)
    val oauthResult = OAuthResult(signUp = signUp)
    coEvery { SignIn.authenticateWithGoogleOneTap(true) } returns ClerkResult.success(oauthResult)

    val result = Auth().signUpWithGoogleOneTap()

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { SignIn.authenticateWithGoogleOneTap(true) }
  }

  @Test
  fun `signUpWithOAuth delegates to sign-up redirect flow`() = runTest {
    mockkObject(SSOService)
    val signUp = mockk<SignUp>(relaxed = true)
    val oauthResult = OAuthResult(signUp = signUp)
    coEvery {
      SSOService.authenticateSignUpWithRedirect(
        strategy = "oauth_google",
        redirectUrl = any(),
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
      )
    } returns ClerkResult.success(oauthResult)

    val result = Auth().signUpWithOAuth(OAuthProvider.GOOGLE)

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      SSOService.authenticateSignUpWithRedirect(
        strategy = "oauth_google",
        redirectUrl = any(),
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
      )
    }
  }

  @Test
  fun `signUp forwards unsafe metadata`() = runTest {
    val signUpApi = mockk<SignUpApi>()
    val createdSignUp = mockk<SignUp>(relaxed = true)
    val createParams = slot<Map<String, String>>()
    mockkObject(ClerkApi)
    every { ClerkApi.signUp } returns signUpApi
    coEvery { signUpApi.createSignUp(capture(createParams)) } returns
      ClerkResult.success(createdSignUp)

    val result =
      Auth().signUp {
        email = "user@example.com"
        unsafeMetadata = mapOf("test" to "test", "nested" to mapOf("active" to true))
      }

    val unsafeMetadata =
      Json.parseToJsonElement(createParams.captured.getValue("unsafe_metadata")).jsonObject
    assertTrue(result is ClerkResult.Success)
    assertSame(createdSignUp, (result as ClerkResult.Success).value)
    assertEquals("user@example.com", createParams.captured["email_address"])
    assertEquals("test", unsafeMetadata.getValue("test").jsonPrimitive.content)
    assertEquals(
      "true",
      unsafeMetadata.getValue("nested").jsonObject.getValue("active").jsonPrimitive.content,
    )
    coVerify(exactly = 1) { signUpApi.createSignUp(any()) }
  }

  @Test
  fun `signUpWithGoogleOneTap emits auth error event on failure`() = runTest {
    mockkObject(SignIn.Companion)
    val error =
      ClerkErrorResponse(
        errors =
          listOf(
            Error(
              code = "external_account_exists",
              message = "Account already exists",
              longMessage = "Account already exists. Use sign in instead.",
            )
          ),
        clerkTraceId = "trace_123",
      )
    coEvery { SignIn.authenticateWithGoogleOneTap(true) } returns ClerkResult.apiFailure(error)

    val auth = Auth()
    val events = mutableListOf<AuthEvent>()
    val eventJob =
      launch(start = CoroutineStart.UNDISPATCHED) { auth.events.take(1).toList(events) }

    val result = auth.signUpWithGoogleOneTap()

    withTimeout(1_000) { eventJob.join() }

    assertTrue(result is ClerkResult.Failure)
    assertEquals(error, (result as ClerkResult.Failure).error)
    assertTrue(events.single() is AuthEvent.Error)
    assertEquals(
      "Account already exists. Use sign in instead.",
      (events.single() as AuthEvent.Error).message,
    )
    coVerify(exactly = 1) { SignIn.authenticateWithGoogleOneTap(true) }
  }

  @Test
  fun `signInWithOtp email creates sign in with strategy without preparing first factor`() =
    runTest {
      val signInApi = mockk<SignInApi>(relaxed = true)
      val createdSignIn = SignIn(id = "sign_in_123")
      val createParams = slot<Map<String, String>>()
      mockkObject(ClerkApi)
      every { ClerkApi.signIn } returns signInApi
      coEvery { signInApi.createSignIn(capture(createParams)) } returns
        ClerkResult.success(createdSignIn)

      val result = Auth().signInWithOtp { email = "user@example.com" }

      assertTrue(result is ClerkResult.Success)
      assertSame(createdSignIn, (result as ClerkResult.Success).value)
      assertEquals("user@example.com", createParams.captured["identifier"])
      assertEquals("email_code", createParams.captured["strategy"])
      assertTrue(createParams.captured.containsKey("locale"))
      coVerify(exactly = 1) { signInApi.createSignIn(any()) }
      coVerify(exactly = 0) { signInApi.prepareSignInFirstFactor(any(), any()) }
    }

  @Test
  fun `signInWithOtp phone creates sign in with strategy without preparing first factor`() =
    runTest {
      val signInApi = mockk<SignInApi>(relaxed = true)
      val createdSignIn = SignIn(id = "sign_in_123")
      val createParams = slot<Map<String, String>>()
      mockkObject(ClerkApi)
      every { ClerkApi.signIn } returns signInApi
      coEvery { signInApi.createSignIn(capture(createParams)) } returns
        ClerkResult.success(createdSignIn)

      val result = Auth().signInWithOtp { phone = "+15555550123" }

      assertTrue(result is ClerkResult.Success)
      assertSame(createdSignIn, (result as ClerkResult.Success).value)
      assertEquals("+15555550123", createParams.captured["identifier"])
      assertEquals("phone_code", createParams.captured["strategy"])
      assertTrue(createParams.captured.containsKey("locale"))
      coVerify(exactly = 1) { signInApi.createSignIn(any()) }
      coVerify(exactly = 0) { signInApi.prepareSignInFirstFactor(any(), any()) }
    }

  @Test
  fun `sessions exposes all sessions on the current client`() {
    val firstSession = testSession("sess_1")
    val secondSession = testSession("sess_2")
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    assertEquals(listOf(firstSession, secondSession), Auth().sessions)
  }

  @Test
  fun `signOut with session ID removes only that session locally`() = runTest {
    val firstSession = testSession("sess_1")
    val secondSession = testSession("sess_2")
    val sessionApi = mockk<SessionApi>()
    val clientApi = mockk<ClientApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    every { ClerkApi.client } returns clientApi
    coEvery { sessionApi.removeSession(firstSession.id) } returns ClerkResult.success(firstSession)
    coEvery { clientApi.get() } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    val result = Auth().signOut(sessionId = firstSession.id)

    assertTrue(result is ClerkResult.Success)
    assertEquals(listOf(secondSession), Clerk.client.sessions)
    assertEquals(secondSession.id, Clerk.client.lastActiveSessionId)
    assertEquals(secondSession, Clerk.sessionFlow.value)
    coVerify(exactly = 1) { sessionApi.removeSession(firstSession.id) }
  }

  @Test
  fun `setActive updates the active session locally`() = runTest {
    val firstSession = testSession("sess_1")
    val secondSession = testSession("sess_2")
    val clientApi = mockk<ClientApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.client } returns clientApi
    coEvery { clientApi.setActive(secondSession.id, "", SET_ACTIVE_INTENT_SELECT_ORG) } returns
      ClerkResult.success(secondSession)
    coEvery { clientApi.get() } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    val result = Auth().setActive(sessionId = secondSession.id)

    assertTrue(result is ClerkResult.Success)
    assertEquals(secondSession.id, Clerk.client.lastActiveSessionId)
    assertEquals(secondSession, Clerk.sessionFlow.value)
    coVerify(exactly = 1) {
      clientApi.setActive(secondSession.id, "", SET_ACTIVE_INTENT_SELECT_ORG)
    }
  }

  @Test
  fun `setActive sends select org intent with active organization id`() = runTest {
    val firstSession = testSession("sess_1")
    val secondSession = testSession("sess_2", lastActiveOrganizationId = "org_2")
    val clientApi = mockk<ClientApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.client } returns clientApi
    coEvery { clientApi.setActive(secondSession.id, "org_2", SET_ACTIVE_INTENT_SELECT_ORG) } returns
      ClerkResult.success(secondSession.copy(lastActiveOrganizationId = null))
    coEvery { clientApi.get() } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    val result = Auth().setActive(sessionId = secondSession.id, organizationId = "org_2")

    assertTrue(result is ClerkResult.Success)
    assertEquals(secondSession.id, Clerk.client.lastActiveSessionId)
    assertEquals("org_2", Clerk.sessionFlow.value?.lastActiveOrganizationId)
    coVerify(exactly = 1) {
      clientApi.setActive(secondSession.id, "org_2", SET_ACTIVE_INTENT_SELECT_ORG)
    }
  }

  @Test
  fun `setActive no-ops personal account selection when organization selection is forced`() =
    runTest {
      setForceOrganizationSelection(true)
      val currentSession = testSession("sess_1", lastActiveOrganizationId = "org_1")
      val personalSession = testSession("sess_2")
      val clientApi = mockk<ClientApi>(relaxed = true)
      mockkObject(ClerkApi)
      every { ClerkApi.client } returns clientApi
      Clerk.updateClient(
        Client(
          id = "client_123",
          sessions = listOf(currentSession, personalSession),
          lastActiveSessionId = currentSession.id,
        )
      )

      val result = Auth().setActive(sessionId = personalSession.id)

      assertTrue(result is ClerkResult.Success)
      assertEquals(currentSession, (result as ClerkResult.Success).value)
      assertEquals(currentSession.id, Clerk.client.lastActiveSessionId)
      assertEquals(currentSession, Clerk.sessionFlow.value)
      coVerify(exactly = 0) { clientApi.setActive(any(), any(), any()) }
    }

  @Test
  fun `setActive restores active session when response client sync clears local sessions`() =
    runTest {
      val firstSession = testSession("sess_1")
      val secondUser = mockk<User>(relaxed = true)
      val secondSession = testSession("sess_2").copy(user = secondUser)
      val dehydratedSecondSession = testSession("sess_2")
      val clientApi = mockk<ClientApi>()
      mockkObject(ClerkApi)
      every { ClerkApi.client } returns clientApi
      coEvery { clientApi.setActive(secondSession.id, "", SET_ACTIVE_INTENT_SELECT_ORG) } coAnswers
        {
          Clerk.updateClient(Client(id = "client_123"))
          ClerkResult.success(dehydratedSecondSession)
        }
      coEvery { clientApi.get() } returns
        ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))
      Clerk.updateClient(
        Client(
          id = "client_123",
          sessions = listOf(firstSession, secondSession),
          lastActiveSessionId = firstSession.id,
        )
      )

      val result = Auth().setActive(sessionId = secondSession.id)

      assertTrue(result is ClerkResult.Success)
      assertEquals(listOf(firstSession, secondSession), Clerk.client.sessions)
      assertEquals(secondSession.id, Clerk.client.lastActiveSessionId)
      assertEquals(secondSession, Clerk.sessionFlow.value)
      assertEquals(secondUser, Clerk.userFlow.value)
    }

  @Test
  fun `setActive keeps active session when follow-up client refresh omits it`() = runTest {
    val firstSession = testSession("sess_1")
    val secondSession = testSession("sess_2", lastActiveOrganizationId = "org_2")
    val clientApi = mockk<ClientApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.client } returns clientApi
    coEvery { clientApi.setActive(secondSession.id, "org_2", SET_ACTIVE_INTENT_SELECT_ORG) } returns
      ClerkResult.success(secondSession.copy(lastActiveOrganizationId = null))
    coEvery { clientApi.get() } returns ClerkResult.success(Client(id = "client_123"))
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    val result = Auth().setActive(sessionId = secondSession.id, organizationId = "org_2")

    assertTrue(result is ClerkResult.Success)
    assertEquals(listOf(firstSession, secondSession), Clerk.client.sessions)
    assertEquals(secondSession.id, Clerk.client.lastActiveSessionId)
    assertEquals(secondSession, Clerk.sessionFlow.value)
    assertEquals("org_2", Clerk.sessionFlow.value?.lastActiveOrganizationId)
  }

  @Test
  fun `setActive keeps active session when follow-up refresh returns stale lastActiveSessionId`() =
    runTest {
      val firstSession = testSession("sess_1")
      val secondSession = testSession("sess_2")
      val clientApi = mockk<ClientApi>()
      mockkObject(ClerkApi)
      every { ClerkApi.client } returns clientApi
      coEvery { clientApi.setActive(secondSession.id, "", SET_ACTIVE_INTENT_SELECT_ORG) } returns
        ClerkResult.success(secondSession)
      // Read replica returns a fully-hydrated client but with the previous
      // lastActiveSessionId — read-after-write lag.
      coEvery { clientApi.get() } returns
        ClerkResult.success(
          Client(
            id = "client_123",
            sessions = listOf(firstSession, secondSession),
            lastActiveSessionId = firstSession.id,
          )
        )
      Clerk.updateClient(
        Client(
          id = "client_123",
          sessions = listOf(firstSession, secondSession),
          lastActiveSessionId = firstSession.id,
        )
      )

      val result = Auth().setActive(sessionId = secondSession.id)

      assertTrue(result is ClerkResult.Success)
      assertEquals(listOf(firstSession, secondSession), Clerk.client.sessions)
      assertEquals(secondSession.id, Clerk.client.lastActiveSessionId)
      assertEquals(secondSession, Clerk.sessionFlow.value)
    }

  private fun setForceOrganizationSelection(enabled: Boolean) {
    Clerk.updateEnvironment(
      Environment(
        authConfig = AuthConfig(singleSessionMode = false),
        displayConfig =
          DisplayConfig(
            applicationName = "Test App",
            branded = true,
            logoImageUrl = "https://example.com/logo.png",
            homeUrl = "/",
            privacyPolicyUrl = null,
            termsUrl = null,
            googleOneTapClientId = null,
          ),
        userSettings =
          UserSettings(
            attributes = emptyMap(),
            signUp =
              UserSettings.SignUpUserSettings(
                customActionRequired = false,
                progressive = false,
                mode = "public",
                legalConsentEnabled = false,
              ),
            social = emptyMap(),
            actions = UserSettings.Actions(),
            passkeySettings = null,
          ),
        organizationSettings = OrganizationSettings(forceOrganizationSelection = enabled),
      )
    )
  }

  private fun testSession(id: String, lastActiveOrganizationId: String? = null): Session =
    Session(
      id = id,
      status = Session.SessionStatus.ACTIVE,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      lastActiveOrganizationId = lastActiveOrganizationId,
      createdAt = 1_000,
      updatedAt = 1_000,
    )
}
