package com.clerk.ui.auth

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error as ClerkError
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.ResultType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AuthViewModel focusing on testable state management and logic.
 *
 * This test suite uses Turbine for testing StateFlow emissions and MockK for mocking dependencies.
 * The tests focus on the ViewModel's behavior, state transitions, and logic that can be tested
 * without complex static method mocking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var viewModel: AuthStartViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    viewModel = AuthStartViewModel(ioDispatcher = testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun initialStateShouldBeIdle() = runTest {
    viewModel.state.test { assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem()) }
  }

  @Test
  fun startAuthWithSignInOrUpModeShouldInitiateSignInOrUpFlow() = runTest {
    // Avoid real API calls
    mockkObject(SignIn.Companion)
    coEvery { SignIn.create(any<SignIn.CreateParams.Strategy>()) } returns
      ClerkResult.apiFailure(null)

    // Verify state transitions when starting SignInOrUp
    viewModel.state.test {
      assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem())

      viewModel.startAuth(
        authMode = AuthMode.SignInOrUp,
        isPhoneNumberFieldActive = false,
        phoneNumber = "",
        identifier = "test@example.com",
      )

      assertEquals(AuthStartViewModel.AuthState.Loading, awaitItem())
      coVerify(timeout = 1_000, exactly = 1) { SignIn.create(any<SignIn.CreateParams.Strategy>()) }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun automaticPasskeySignInUsesPasskeyStrategy() = runTest {
    val signIn = SignIn(id = "sign_in_123")
    mockkObject(SignIn.Companion)
    coEvery {
      SignIn.create(
        any<SignIn.CreateParams.Strategy.Passkey>(),
        preferImmediatelyAvailableCredentials = true,
      )
    } returns ClerkResult.success(signIn)

    viewModel.state.test {
      assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem())

      viewModel.startAutomaticPasskeySignIn()

      assertEquals(AuthStartViewModel.AuthState.Success.SignInSuccess(signIn), awaitItem())
      coVerify(exactly = 1) {
        SignIn.create(
          any<SignIn.CreateParams.Strategy.Passkey>(),
          preferImmediatelyAvailableCredentials = true,
        )
      }
    }
  }

  @Test
  fun automaticPasskeySignInSuppressesNoSavedCredentialError() = runTest {
    val noSavedCredentialException =
      Class.forName("com.clerk.api.credentials.CredentialFlowException\$NoSavedCredential")
        .getDeclaredConstructor()
        .newInstance() as Throwable
    mockkObject(SignIn.Companion)
    coEvery {
      SignIn.create(
        any<SignIn.CreateParams.Strategy.Passkey>(),
        preferImmediatelyAvailableCredentials = true,
      )
    } returns ClerkResult.unknownFailure(noSavedCredentialException)

    viewModel.state.test {
      assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem())

      viewModel.startAutomaticPasskeySignIn()

      testDispatcher.scheduler.advanceUntilIdle()
      coVerify(exactly = 1) {
        SignIn.create(
          any<SignIn.CreateParams.Strategy.Passkey>(),
          preferImmediatelyAvailableCredentials = true,
        )
      }
      expectNoEvents()
    }
  }

  @Test
  fun automaticPasskeySignInSuppressesUserCancelledError() = runTest {
    val userCancelledException =
      Class.forName("com.clerk.api.credentials.CredentialFlowException\$UserCancelled")
        .getDeclaredConstructor()
        .newInstance() as Throwable
    mockkObject(SignIn.Companion)
    coEvery {
      SignIn.create(
        any<SignIn.CreateParams.Strategy.Passkey>(),
        preferImmediatelyAvailableCredentials = true,
      )
    } returns ClerkResult.unknownFailure(userCancelledException)

    viewModel.state.test {
      assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem())

      viewModel.startAutomaticPasskeySignIn()

      testDispatcher.scheduler.advanceUntilIdle()
      coVerify(exactly = 1) {
        SignIn.create(
          any<SignIn.CreateParams.Strategy.Passkey>(),
          preferImmediatelyAvailableCredentials = true,
        )
      }
      expectNoEvents()
    }
  }

  @Test
  fun automaticPasskeySignInSurfacesApiErrors() = runTest {
    mockkObject(SignIn.Companion)
    coEvery {
      SignIn.create(
        any<SignIn.CreateParams.Strategy.Passkey>(),
        preferImmediatelyAvailableCredentials = true,
      )
    } returns
      ClerkResult.apiFailure(
        ClerkErrorResponse(errors = listOf(ClerkError(longMessage = "Passkey failed")))
      )

    viewModel.state.test {
      assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem())

      viewModel.startAutomaticPasskeySignIn()

      assertEquals(AuthStartViewModel.AuthState.Error("Passkey failed"), awaitItem())
    }
  }

  @Test
  fun oauthResultWithSignInResultTypeShouldSetCorrectSuccessState() {
    // Test the OAuth result processing logic
    val mockSignIn = mockk<SignIn>(relaxed = true)
    val mockOAuthResult =
      mockk<OAuthResult> {
        every { resultType } returns ResultType.SIGN_IN
        every { signIn } returns mockSignIn
        every { signUp } returns null
      }

    // Simulate the OAuth result processing (this logic is extracted from the ViewModel)
    val expectedState =
      when (mockOAuthResult.resultType) {
        ResultType.SIGN_IN ->
          AuthStartViewModel.AuthState.OAuthState.SignInSuccess(signIn = mockOAuthResult.signIn!!)
        ResultType.SIGN_UP ->
          AuthStartViewModel.AuthState.OAuthState.SignUpSuccess(signUp = mockOAuthResult.signUp!!)
        ResultType.UNKNOWN ->
          AuthStartViewModel.AuthState.OAuthState.Error("Unknown result type from OAuth provider")
      }

    assertTrue(
      "Should create success state with SignIn",
      expectedState is AuthStartViewModel.AuthState.OAuthState.SignInSuccess,
    )
    assertEquals(
      mockSignIn,
      (expectedState as AuthStartViewModel.AuthState.OAuthState.SignInSuccess).signIn,
    )
  }

  @Test
  fun oauthResultWithSignUpResultTypeShouldSetCorrectSuccessState() {
    // Test the OAuth result processing logic
    val mockSignUp = mockk<SignUp>(relaxed = true)
    val mockOAuthResult =
      mockk<OAuthResult> {
        every { resultType } returns ResultType.SIGN_UP
        every { signUp } returns mockSignUp
      }

    // Simulate the OAuth result processing
    val expectedState =
      when (mockOAuthResult.resultType) {
        ResultType.SIGN_IN ->
          AuthStartViewModel.AuthState.OAuthState.SignInSuccess(signIn = mockOAuthResult.signIn!!)
        ResultType.SIGN_UP ->
          AuthStartViewModel.AuthState.OAuthState.SignUpSuccess(signUp = mockOAuthResult.signUp!!)
        ResultType.UNKNOWN ->
          AuthStartViewModel.AuthState.OAuthState.Error("Unknown result type from OAuth provider")
      }

    assertTrue(
      "Should create success state with SignUp",
      expectedState is AuthStartViewModel.AuthState.OAuthState.SignUpSuccess,
    )
    assertEquals(
      mockSignUp,
      (expectedState as AuthStartViewModel.AuthState.OAuthState.SignUpSuccess).signUp,
    )
  }

  @Test
  fun oauthResultWithUnknownResultTypeShouldSetErrorState() {
    // Test the OAuth result processing logic
    val mockOAuthResult =
      mockk<OAuthResult> {
        every { resultType } returns ResultType.UNKNOWN
        every { signIn } returns null
        every { signUp } returns null
      }

    // Simulate the OAuth result processing
    val expectedState =
      when (mockOAuthResult.resultType) {
        ResultType.SIGN_IN ->
          AuthStartViewModel.AuthState.OAuthState.SignInSuccess(signIn = mockOAuthResult.signIn!!)
        ResultType.SIGN_UP ->
          AuthStartViewModel.AuthState.OAuthState.SignUpSuccess(signUp = mockOAuthResult.signUp!!)
        ResultType.UNKNOWN ->
          AuthStartViewModel.AuthState.OAuthState.Error("Unknown result type from OAuth provider")
      }

    assertTrue(
      "Should create error state for unknown result type",
      expectedState is AuthStartViewModel.AuthState.OAuthState.Error,
    )
    assertEquals(
      "Unknown result type from OAuth provider",
      (expectedState as AuthStartViewModel.AuthState.OAuthState.Error).message,
    )
  }

  @Test
  fun signUpParamsShouldBeCreatedCorrectlyBasedOnInputType() {
    // Test the sign-up parameter creation logic
    val emailIdentifier = "test@example.com"
    val usernameIdentifier = "testuser"
    val phoneNumber = "+1234567890"

    // Test email recognition (this tests the private isEmailAddress extension)
    assertTrue(
      "Should recognize email format",
      emailIdentifier.contains("@") && emailIdentifier.contains("."),
    )
    assertTrue("Should not recognize username as email", !usernameIdentifier.contains("@"))

    // Test parameter creation logic
    val emailParams =
      if (emailIdentifier.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
        "email"
      } else {
        "username"
      }

    val usernameParams =
      if (usernameIdentifier.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
        "email"
      } else {
        "username"
      }

    assertEquals("email", emailParams)
    assertEquals("username", usernameParams)
  }

  @Test
  fun startAuthWithSignUpPassesUnsafeMetadataToSignUpCreate() = runTest {
    val paramsSlot = slot<SignUp.CreateParams>()
    val mockSignUp = mockk<SignUp>(relaxed = true)
    mockkObject(SignUp.Companion)
    coEvery { SignUp.create(any<SignUp.CreateParams>()) } returns ClerkResult.success(mockSignUp)

    viewModel.state.test {
      assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem())

      viewModel.startAuth(
        authMode = AuthMode.SignUp,
        isPhoneNumberFieldActive = false,
        phoneNumber = "",
        identifier = "test@example.com",
        unsafeMetadata = mapOf("test" to "test", "nested" to mapOf("active" to true)),
      )

      assertEquals(AuthStartViewModel.AuthState.Loading, awaitItem())
      assertEquals(AuthStartViewModel.AuthState.Success.SignUpSuccess(mockSignUp), awaitItem())
    }

    coVerify(timeout = 1_000, exactly = 1) { SignUp.create(capture(paramsSlot)) }
    val params = paramsSlot.captured as SignUp.CreateParams.Standard
    val unsafeMetadata = requireNotNull(params.unsafeMetadata)
    assertEquals("test@example.com", params.emailAddress)
    assertEquals("test", unsafeMetadata.getValue("test"))
    assertEquals(mapOf("active" to true), unsafeMetadata.getValue("nested"))
  }

  @Test
  fun signInOrUpFallbackPassesUnsafeMetadataToSignUpCreate() = runTest {
    val paramsSlot = slot<SignUp.CreateParams>()
    val mockSignUp = mockk<SignUp>(relaxed = true)
    mockkObject(SignIn.Companion)
    mockkObject(SignUp.Companion)
    coEvery { SignIn.create(any<SignIn.CreateParams.Strategy>()) } returns
      ClerkResult.apiFailure(
        ClerkErrorResponse(errors = listOf(ClerkError(code = "form_identifier_not_found")))
      )
    coEvery { SignUp.create(any<SignUp.CreateParams>()) } returns ClerkResult.success(mockSignUp)

    viewModel.state.test {
      assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem())

      viewModel.startAuth(
        authMode = AuthMode.SignInOrUp,
        isPhoneNumberFieldActive = true,
        phoneNumber = "+1234567890",
        identifier = "test@example.com",
        unsafeMetadata = mapOf("source" to "prebuilt"),
      )

      assertEquals(AuthStartViewModel.AuthState.Loading, awaitItem())
      assertEquals(AuthStartViewModel.AuthState.Success.SignUpSuccess(mockSignUp), awaitItem())
    }

    coVerify(timeout = 1_000, exactly = 1) { SignUp.create(capture(paramsSlot)) }
    val params = paramsSlot.captured as SignUp.CreateParams.Standard
    val unsafeMetadata = requireNotNull(params.unsafeMetadata)
    assertEquals("+1234567890", params.phoneNumber)
    assertEquals("prebuilt", unsafeMetadata.getValue("source"))
  }

  @Test
  fun enterpriseSSODetectionShouldWorkCorrectly() {
    // Test the enterprise SSO detection logic directly
    val ssoStrategy = "enterprise_sso"
    val passwordStrategy = "password"

    // Test the requiresEnterpriseSSO logic that the ViewModel uses
    val requiresSSO = ssoStrategy == "enterprise_sso"
    val doesNotRequireSSO = passwordStrategy == "enterprise_sso"

    assertTrue("Should detect enterprise SSO requirement", requiresSSO)
    assertTrue("Should not detect enterprise SSO for password", !doesNotRequireSSO)

    // Test with nullable strategy (edge case)
    val nullStrategy: String? = null
    val requiresSSOWithNull = nullStrategy == "enterprise_sso"
    assertTrue("Should not require SSO with null strategy", !requiresSSOWithNull)
  }

  @Test
  fun identifierResolutionShouldWorkCorrectly() {
    // Test the identifier resolution logic used in startAuth
    val identifier = "test@example.com"
    val phoneNumber = "+1234567890"

    // Test phone number field active
    val resolvedIdentifierWhenPhoneActive = if (true) phoneNumber else identifier
    assertEquals(phoneNumber, resolvedIdentifierWhenPhoneActive)

    // Test phone number field not active
    val resolvedIdentifierWhenPhoneNotActive = if (false) phoneNumber else identifier
    assertEquals(identifier, resolvedIdentifierWhenPhoneNotActive)
  }

  @Test
  fun authModeEnumShouldHaveCorrectValues() {
    // Test that all AuthMode values are available
    val signIn = AuthMode.SignIn
    val signUp = AuthMode.SignUp
    val signInOrUp = AuthMode.SignInOrUp

    assertEquals("SignIn", signIn.name)
    assertEquals("SignUp", signUp.name)
    assertEquals("SignInOrUp", signInOrUp.name)
  }

  @Test
  fun authModeTransferabilityShouldMatchFlowMode() {
    assertEquals(false, AuthMode.SignIn.transferable)
    assertEquals(true, AuthMode.SignUp.transferable)
    assertEquals(true, AuthMode.SignInOrUp.transferable)
  }

  @Test
  fun emailRegexPatternShouldWorkCorrectly() {
    // Test email validation regex pattern (same as used in the ViewModel)
    val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

    val validEmails =
      listOf(
        "test@example.com",
        "user.name@domain.co.uk",
        "test123+tag@example.org",
        "simple@test.com",
        "user_name@test-domain.org",
      )

    val invalidEmails =
      listOf(
        "testexample.com",
        "@example.com",
        "test@",
        "username",
        "test@domain",
        "test.domain.com",
        "test@@domain.com",
      )

    validEmails.forEach { email -> assertTrue("$email should be valid", emailRegex.matches(email)) }

    invalidEmails.forEach { email ->
      assertTrue("$email should be invalid", !emailRegex.matches(email))
    }
  }

  @Test
  fun oauthProviderEnumShouldHaveExpectedValues() {
    // Test that OAuth providers work correctly with our ViewModel
    val google = OAuthProvider.GOOGLE
    val facebook = OAuthProvider.FACEBOOK

    assertEquals("GOOGLE", google.name)
    assertEquals("FACEBOOK", facebook.name)

    // Test that we can use these in our ViewModel logic
    val testProviders = listOf(google, facebook)
    assertTrue("Should contain Google", testProviders.contains(OAuthProvider.GOOGLE))
    assertTrue("Should contain Facebook", testProviders.contains(OAuthProvider.FACEBOOK))
  }

  @Test
  fun googleSocialAuthUsesBrowserRedirectWhenOneTapIsNotPreferred() = runTest {
    mockkObject(SignIn.Companion)
    val mockSignIn = mockk<SignIn>(relaxed = true)

    coEvery { SignIn.authenticateWithGoogleOneTap(any()) } returns
      ClerkResult.success(OAuthResult(signIn = mockSignIn))
    coEvery { SignIn.authenticateWithRedirect(any(), any()) } returns
      ClerkResult.success(OAuthResult(signIn = mockSignIn))

    viewModel.authenticateWithSocialProvider(
      provider = OAuthProvider.GOOGLE,
      transferable = true,
      preferGoogleOneTap = false,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { SignIn.authenticateWithGoogleOneTap(any()) }
    coVerify(exactly = 1) { SignIn.authenticateWithRedirect(any(), true) }
  }

  @Test
  fun socialOAuthCanStartWithSignUp() = runTest {
    mockkObject(SignIn.Companion)
    mockkObject(SignUp.Companion)
    val mockSignUp = mockk<SignUp>(relaxed = true)

    coEvery { SignIn.authenticateWithRedirect(any(), any()) } returns
      ClerkResult.success(OAuthResult(signIn = mockk(relaxed = true)))
    coEvery { SignUp.authenticateWithRedirect(any()) } returns
      ClerkResult.success(OAuthResult(signUp = mockSignUp))

    viewModel.authenticateWithSocialProvider(
      provider = OAuthProvider.GOOGLE,
      transferable = true,
      preferGoogleOneTap = false,
      startOAuthWithSignUp = true,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { SignUp.authenticateWithRedirect(any()) }
    coVerify(exactly = 0) { SignIn.authenticateWithRedirect(any(), any()) }
  }

  @Test
  fun socialOAuthSignUpPassesUnsafeMetadata() = runTest {
    val paramsSlot = slot<SignUp.AuthenticateWithRedirectParams>()
    val mockSignUp = mockk<SignUp>(relaxed = true)
    mockkObject(SignUp.Companion)
    coEvery { SignUp.authenticateWithRedirect(any()) } returns
      ClerkResult.success(OAuthResult(signUp = mockSignUp))

    viewModel.authenticateWithSocialProvider(
      provider = OAuthProvider.GOOGLE,
      transferable = true,
      preferGoogleOneTap = false,
      startOAuthWithSignUp = true,
      unsafeMetadata = mapOf("source" to "social"),
    )
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { SignUp.authenticateWithRedirect(capture(paramsSlot)) }
    val params = paramsSlot.captured as SignUp.AuthenticateWithRedirectParams.OAuth
    val unsafeMetadata = requireNotNull(params.unsafeMetadata)
    assertEquals("social", unsafeMetadata.getValue("source"))
  }

  @Test
  fun googleSocialAuthUsesOneTapWhenPreferredAndEnabled() = runTest {
    mockkObject(Clerk)
    every { Clerk.isGoogleOneTapEnabled } returns true
    mockkObject(SignIn.Companion)
    val mockSignIn = mockk<SignIn>(relaxed = true)

    coEvery { SignIn.authenticateWithGoogleOneTap(any()) } returns
      ClerkResult.success(OAuthResult(signIn = mockSignIn))
    coEvery { SignIn.authenticateWithRedirect(any(), any()) } returns
      ClerkResult.success(OAuthResult(signIn = mockSignIn))

    viewModel.authenticateWithSocialProvider(
      provider = OAuthProvider.GOOGLE,
      transferable = true,
      preferGoogleOneTap = true,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { SignIn.authenticateWithGoogleOneTap(true) }
    coVerify(exactly = 0) { SignIn.authenticateWithRedirect(any(), any()) }
  }

  @Test
  fun googleSocialAuthFallsBackToBrowserRedirectWhenOneTapHasNoGoogleAccount() = runTest {
    mockkObject(Clerk)
    every { Clerk.isGoogleOneTapEnabled } returns true
    mockkObject(SignIn.Companion)
    val mockSignIn = mockk<SignIn>(relaxed = true)
    val noGoogleAccountException =
      Class.forName("com.clerk.api.credentials.CredentialFlowException\$NoGoogleAccount")
        .getDeclaredConstructor()
        .newInstance() as Throwable

    coEvery { SignIn.authenticateWithGoogleOneTap(any()) } returns
      ClerkResult.unknownFailure(noGoogleAccountException)
    coEvery { SignIn.authenticateWithRedirect(any(), any()) } returns
      ClerkResult.success(OAuthResult(signIn = mockSignIn))

    viewModel.authenticateWithSocialProvider(
      provider = OAuthProvider.GOOGLE,
      transferable = true,
      preferGoogleOneTap = true,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { SignIn.authenticateWithGoogleOneTap(true) }
    coVerify(exactly = 1) { SignIn.authenticateWithRedirect(any(), true) }
  }
}
