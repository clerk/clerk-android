package com.clerk.ui.auth

import com.clerk.api.*
import com.clerk.testing.mockClerk
import com.clerk.testing.testCoreError
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private lateinit var clerk: Clerk
  private lateinit var viewModel: AuthStartViewModel
  private val signIn = mockk<SignIn>(relaxed = true)
  private val signUp = mockk<SignUp>(relaxed = true)
  private val signInResult = MobileAuthenticationResult.Case1(MobileAuthCallbackResultCase1(signIn))
  private val signUpResult = MobileAuthenticationResult.Case2(MobileAuthCallbackResultCase2(signUp))

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    clerk = mockClerk()
    every { clerk.signIn } returns signIn
    every { signIn.supportedFirstFactors } returns emptyList()
    coEvery { clerk.startAuthentication(any()) } returns signInResult
    coEvery { clerk.authenticateWithSSO(any()) } returns signInResult
    viewModel = AuthStartViewModel(clerk)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  private fun start(
    mode: AuthMode = AuthMode.SignIn,
    identifier: String = "test@example.com",
    phone: String? = null,
    metadata: JsonObject? = null,
  ) {
    viewModel.startAuth(mode, phone != null, phone.orEmpty(), identifier, metadata)
  }

  private fun finish() = dispatcher.scheduler.advanceUntilIdle()

  private fun identifierParams(): MobileIdentifierParams {
    val params = slot<MobileIdentifierParams>()
    coVerify(exactly = 1) { clerk.startAuthentication(capture(params)) }
    return params.captured
  }

  private fun socialParams(): MobileSSOParams {
    val params = slot<MobileSSOParams>()
    coVerify(exactly = 1) { clerk.authenticateWithSSO(capture(params)) }
    return params.captured
  }

  @Test
  fun initialStateShouldBeIdle() {
    assertEquals(AuthStartViewModel.AuthState.Idle, viewModel.state.value)
  }

  @Test
  fun startAuthWithSignInOrUpModeShouldInitiateSignInOrUpFlow() {
    start(AuthMode.SignInOrUp)
    assertEquals(AuthStartViewModel.AuthState.Loading, viewModel.state.value)
    finish()
    assertEquals(MobileIdentifierParamsMode.SignInOrUp, identifierParams().mode)
    assertEquals(AuthStartViewModel.AuthState.Success.SignInSuccess(signIn), viewModel.state.value)
  }

  @Test
  fun startAuthWithSignInModeShouldSurfaceApiFailure() {
    coEvery { clerk.startAuthentication(any()) } throws testCoreError("Couldn't find your account.")
    start()
    finish()
    assertEquals(
      AuthStartViewModel.AuthState.Error("Couldn't find your account."),
      viewModel.state.value,
    )
  }

  @Test
  fun startAuthWithSignInModeShouldSurfaceUnknownFailure() {
    coEvery { clerk.startAuthentication(any()) } throws IllegalStateException("Network unavailable")
    start()
    finish()
    assertEquals(AuthStartViewModel.AuthState.Error("Network unavailable"), viewModel.state.value)
  }

  @Test
  fun automaticPasskeySignInUsesPasskeyStrategy() {
    coEvery { signIn.passkey(any()) } returns mockk(relaxed = true)
    viewModel.startAutomaticPasskeySignIn()
    finish()
    coVerify {
      signIn.passkey(
        SignInPasskeyParams(
          flow = SignInPasskeyParamsFlow.Discoverable,
          preferImmediatelyAvailableCredentials = true,
        )
      )
    }
    assertEquals(AuthStartViewModel.AuthState.Success.SignInSuccess(signIn), viewModel.state.value)
  }

  @Test
  fun automaticPasskeySignInSuppressesNoSavedCredentialError() {
    coEvery { signIn.passkey(any()) } throws
      CoreException("no_saved_credential", passkeyStage = "gettingCredential")
    viewModel.startAutomaticPasskeySignIn()
    finish()
    assertEquals(AuthStartViewModel.AuthState.Idle, viewModel.state.value)
  }

  @Test
  fun automaticPasskeySignInSuppressesUserCancelledError() {
    coEvery { signIn.passkey(any()) } throws
      CoreException("user_cancelled", passkeyStage = "attemptingFirstFactor")
    viewModel.startAutomaticPasskeySignIn()
    finish()
    assertEquals(AuthStartViewModel.AuthState.Idle, viewModel.state.value)
  }

  @Test
  fun automaticPasskeySignInSurfacesApiErrors() {
    coEvery { signIn.passkey(any()) } throws
      CoreException("passkey_failed", "Passkey failed", passkeyStage = "attemptingFirstFactor")
    viewModel.startAutomaticPasskeySignIn()
    finish()
    assertEquals(AuthStartViewModel.AuthState.Error("Passkey failed"), viewModel.state.value)
  }

  @Test
  fun automaticPasskeySignInDoesNotDuplicatePendingRequest() {
    coEvery { signIn.passkey(any()) } coAnswers { awaitCancellation() }
    viewModel.startAutomaticPasskeySignIn()
    dispatcher.scheduler.runCurrent()
    viewModel.startAutomaticPasskeySignIn()
    dispatcher.scheduler.runCurrent()
    coVerify(exactly = 1) { signIn.passkey(any()) }
    viewModel.cancelAutomaticPasskeySignIn()
    finish()
    assertEquals(AuthStartViewModel.AuthState.Idle, viewModel.state.value)
  }

  @Test
  fun explicitAuthenticationCancelsAutomaticPasskey() {
    var cancelled = false
    coEvery { signIn.passkey(any()) } coAnswers
      {
        try {
          awaitCancellation()
        } finally {
          cancelled = true
        }
      }
    viewModel.startAutomaticPasskeySignIn()
    dispatcher.scheduler.runCurrent()
    start()
    finish()
    assertTrue(cancelled)
    assertEquals(AuthStartViewModel.AuthState.Success.SignInSuccess(signIn), viewModel.state.value)
  }

  @Test
  fun oauthResultWithSignInResultTypeShouldSetCorrectSuccessState() {
    viewModel.authenticateWithSocialProvider(OAuthProvider.Google)
    assertEquals(AuthStartViewModel.AuthState.OAuthState.Loading, viewModel.state.value)
    finish()
    assertEquals(
      AuthStartViewModel.AuthState.OAuthState.SignInSuccess(signIn),
      viewModel.state.value,
    )
  }

  @Test
  fun oauthResultWithSignUpResultTypeShouldSetCorrectSuccessState() {
    coEvery { clerk.authenticateWithSSO(any()) } returns signUpResult
    viewModel.authenticateWithSocialProvider(OAuthProvider.Google)
    finish()
    assertEquals(
      AuthStartViewModel.AuthState.OAuthState.SignUpSuccess(signUp),
      viewModel.state.value,
    )
  }

  @Test
  fun invalidOAuthResultShouldSetErrorState() {
    // Invalid wire unions fail in the generated decoder before reaching presentation.
    coEvery { clerk.authenticateWithSSO(any()) } throws
      CoreException("protocol_error", "Invalid authentication result")
    viewModel.authenticateWithSocialProvider(OAuthProvider.Google)
    finish()
    assertEquals(
      AuthStartViewModel.AuthState.OAuthState.Error("Invalid authentication result"),
      viewModel.state.value,
    )
  }

  @Test
  fun signUpParamsShouldBeCreatedCorrectlyBasedOnInputType() {
    start(AuthMode.SignUp, "testuser")
    finish()
    val params = identifierParams()
    assertEquals(MobileIdentifierParamsMode.SignUp, params.mode)
    assertEquals(MobileIdentifierParamsIdentifierType.Username, params.identifierType)
    assertEquals("testuser", params.identifier)
  }

  @Test
  fun startAuthWithSignUpPassesUnsafeMetadataToCore() {
    val metadata = buildJsonObject {
      put("test", "test")
      putJsonObject("nested") { put("active", true) }
    }
    coEvery { clerk.startAuthentication(any()) } returns signUpResult
    start(AuthMode.SignUp, metadata = metadata)
    finish()
    assertEquals(metadata, identifierParams().unsafeMetadata)
    assertEquals(AuthStartViewModel.AuthState.Success.SignUpSuccess(signUp), viewModel.state.value)
  }

  @Test
  fun signInOrUpFallbackPreservesMetadataAndPhoneSelection() {
    val metadata = buildJsonObject { put("source", "prebuilt") }
    // Account discovery and transfer are owned by the TypeScript core.
    coEvery { clerk.startAuthentication(any()) } returns signUpResult
    start(AuthMode.SignInOrUp, phone = "+1234567890", metadata = metadata)
    finish()
    val params = identifierParams()
    assertEquals(metadata, params.unsafeMetadata)
    assertEquals("+1234567890", params.identifier)
    assertEquals(MobileIdentifierParamsIdentifierType.PhoneNumber, params.identifierType)
    assertEquals(AuthStartViewModel.AuthState.Success.SignUpSuccess(signUp), viewModel.state.value)
  }

  private fun enterpriseFactor() {
    every { signIn.identifier } returns "user@example.com"
    every { signIn.supportedFirstFactors } returns
      listOf(SignInFirstFactor.Case8(EnterpriseSSOFactor(enterpriseConnectionId = "sso_123")))
  }

  @Test
  fun enterpriseSSODetectionShouldStartCoreBrowserFlow() {
    enterpriseFactor()
    start()
    finish()
    val params = socialParams()
    assertEquals(SignInSSOParamsStrategy.EnterpriseSso, params.strategy)
    assertEquals("sso_123", params.enterpriseConnectionId)
    assertEquals("user@example.com", params.identifier)
    assertEquals(false, params.transferable)
    assertEquals(AuthStartViewModel.AuthState.Success.SignInSuccess(signIn), viewModel.state.value)
  }

  @Test
  fun identifierResolutionShouldUseEmailWhenPhoneInactive() {
    start()
    finish()
    assertEquals("test@example.com", identifierParams().identifier)
    assertEquals(
      MobileIdentifierParamsIdentifierType.EmailAddress,
      identifierParams().identifierType,
    )
  }

  @Test
  fun authModeEnumShouldHaveCorrectValues() {
    assertEquals(listOf("SignIn", "SignUp", "SignInOrUp"), AuthMode.entries.map { it.name })
  }

  @Test
  fun authModeTransferabilityShouldMatchFlowMode() {
    assertFalse(AuthMode.SignIn.transferable)
    assertTrue(AuthMode.SignUp.transferable)
    assertTrue(AuthMode.SignInOrUp.transferable)
  }

  @Test
  fun emailInputClassificationShouldWorkCorrectly() {
    val emails =
      listOf(
        "test@example.com",
        "user.name@domain.co.uk",
        "test123+tag@example.org",
        "simple@test.com",
        "user_name@test-domain.org",
      )
    val usernames =
      listOf(
        "testexample.com",
        "@example.com",
        "test@",
        "username",
        "test@domain",
        "test.domain.com",
        "test@@domain.com",
      )
    (emails + usernames).forEach { value ->
      clearMocks(clerk, answers = false, childMocks = false)
      start(identifier = value)
      finish()
      assertEquals(
        if (value in emails) MobileIdentifierParamsIdentifierType.EmailAddress
        else MobileIdentifierParamsIdentifierType.Username,
        identifierParams().identifierType,
      )
    }
  }

  @Test
  fun oauthProviderShouldHaveExpectedValues() {
    assertEquals("google", OAuthProvider.Google.rawValue)
    assertEquals("facebook", OAuthProvider.Facebook.rawValue)
  }

  @Test
  fun googleSocialAuthUsesBrowserRedirectWhenOneTapIsNotPreferred() {
    viewModel.authenticateWithSocialProvider(OAuthProvider.Google, preferGoogleOneTap = false)
    finish()
    assertEquals(false, socialParams().preferGoogleOneTap)
    assertEquals("oauth_google", socialParams().strategy.rawValue)
  }

  @Test
  fun customSocialAuthPreservesProviderStrategy() {
    viewModel.authenticateWithSocialProvider(
      OAuthProvider.Unrecognized("custom_patreon"),
      preferGoogleOneTap = false,
    )
    finish()
    assertEquals("oauth_custom_patreon", socialParams().strategy.rawValue)
  }

  @Test
  fun socialOAuthCancellationReturnsToIdle() {
    coEvery { clerk.authenticateWithSSO(any()) } throws CoreException("user_cancelled")
    viewModel.authenticateWithSocialProvider(OAuthProvider.Github)
    finish()
    assertEquals(AuthStartViewModel.AuthState.Idle, viewModel.state.value)
  }

  @Test
  fun enterpriseSSOCancellationReturnsToIdle() {
    enterpriseFactor()
    coEvery { clerk.authenticateWithSSO(any()) } throws CoreException("user_cancelled")
    start()
    finish()
    assertEquals(false, socialParams().transferable)
    assertEquals(AuthStartViewModel.AuthState.Idle, viewModel.state.value)
  }

  @Test
  fun socialOAuthCanStartWithSignUp() {
    coEvery { clerk.authenticateWithSSO(any()) } returns signUpResult
    viewModel.authenticateWithSocialProvider(OAuthProvider.Google, startOAuthWithSignUp = true)
    finish()
    assertEquals(MobileSSOParamsStart.SignUp, socialParams().start)
    assertEquals(
      AuthStartViewModel.AuthState.OAuthState.SignUpSuccess(signUp),
      viewModel.state.value,
    )
  }

  @Test
  fun socialOAuthSignUpPassesUnsafeMetadata() {
    val metadata = buildJsonObject { put("source", "social") }
    viewModel.authenticateWithSocialProvider(
      OAuthProvider.Google,
      startOAuthWithSignUp = true,
      unsafeMetadata = metadata,
    )
    finish()
    assertEquals(metadata, socialParams().unsafeMetadata)
  }

  @Test
  fun googleSocialAuthPassesOneTapPreferenceToCore() {
    viewModel.authenticateWithSocialProvider(OAuthProvider.Google, preferGoogleOneTap = true)
    finish()
    assertEquals(true, socialParams().preferGoogleOneTap)
    assertEquals(true, socialParams().transferable)
  }

  @Test
  fun googleSocialAuthAcceptsCoreBrowserFallbackResult() {
    // Capability availability and One Tap fallback are exercised in mobile-runtime SSO tests.
    viewModel.authenticateWithSocialProvider(OAuthProvider.Google, preferGoogleOneTap = true)
    finish()
    coVerify(exactly = 1) { clerk.authenticateWithSSO(any()) }
    assertEquals(
      AuthStartViewModel.AuthState.OAuthState.SignInSuccess(signIn),
      viewModel.state.value,
    )
  }
}
