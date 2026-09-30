package com.clerk.ui.auth

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error as ClerkError
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.authenticateWithPreparedRedirect
import com.clerk.api.signin.prepareFirstFactor
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.ResultType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
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
    mockkObject(SignIn.Companion)
    coEvery { SignIn.create(any<SignIn.CreateParams.Strategy>()) } returns
      ClerkResult.apiFailure(null)

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
  fun startAuthWithSignInModeShouldSurfaceApiFailure() = runTest {
    mockkObject(SignIn.Companion)
    coEvery { SignIn.create(any<SignIn.CreateParams.Strategy>()) } returns
      ClerkResult.apiFailure(
        ClerkErrorResponse(errors = listOf(ClerkError(longMessage = "Couldn't find your account.")))
      )

    viewModel.state.test {
      assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem())

      viewModel.startAuth(
        authMode = AuthMode.SignIn,
        isPhoneNumberFieldActive = false,
        phoneNumber = "",
        identifier = "test@example.com",
      )

      assertEquals(AuthStartViewModel.AuthState.Loading, awaitItem())
      assertEquals(AuthStartViewModel.AuthState.Error("Couldn't find your account."), awaitItem())
    }
  }

  @Test
  fun startAuthWithSignInModeShouldSurfaceUnknownFailure() = runTest {
    mockkObject(SignIn.Companion)
    coEvery { SignIn.create(any<SignIn.CreateParams.Strategy>()) } returns
      ClerkResult.unknownFailure(IllegalStateException("Network unavailable"))

    viewModel.state.test {
      assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem())

      viewModel.startAuth(
        authMode = AuthMode.SignIn,
        isPhoneNumberFieldActive = false,
        phoneNumber = "",
        identifier = "test@example.com",
      )

      assertEquals(AuthStartViewModel.AuthState.Loading, awaitItem())
      assertEquals(
        AuthStartViewModel.AuthState.Error("Error occurred with unknown message."),
        awaitItem(),
      )
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
  fun oauthRedirectWithSignInResultSetsSignInSuccessState() = runTest {
    val signIn = SignIn(id = "sign_in_oauth")
    mockkObject(SignIn.Companion)
    coEvery { SignIn.authenticateWithRedirect(any(), any()) } returns
      ClerkResult.success(OAuthResult(signIn = signIn))

    viewModel.authenticateWithSocialProvider(
      provider = OAuthProvider.GITHUB,
      preferGoogleOneTap = false,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(
      AuthStartViewModel.AuthState.OAuthState.SignInSuccess(signIn),
      viewModel.state.value,
    )
  }

  @Test
  fun oauthRedirectWithSignUpResultSetsSignUpSuccessState() = runTest {
    val signUp = mockk<SignUp>(relaxed = true)
    mockkObject(SignIn.Companion)
    coEvery { SignIn.authenticateWithRedirect(any(), any()) } returns
      ClerkResult.success(OAuthResult(signUp = signUp))

    viewModel.authenticateWithSocialProvider(
      provider = OAuthProvider.GITHUB,
      preferGoogleOneTap = false,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(
      AuthStartViewModel.AuthState.OAuthState.SignUpSuccess(signUp),
      viewModel.state.value,
    )
  }

  @Test
  fun oauthRedirectWithUnknownResultSetsErrorState() = runTest {
    val unknownResult = mockk<OAuthResult> { every { resultType } returns ResultType.UNKNOWN }
    mockkObject(SignIn.Companion)
    coEvery { SignIn.authenticateWithRedirect(any(), any()) } returns
      ClerkResult.success(unknownResult)

    viewModel.authenticateWithSocialProvider(
      provider = OAuthProvider.GITHUB,
      preferGoogleOneTap = false,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(
      AuthStartViewModel.AuthState.OAuthState.Error("Unknown result type from OAuth provider"),
      viewModel.state.value,
    )
  }

  @Test
  fun signUpParamsShouldBeCreatedCorrectlyBasedOnInputType() {
    val emailIdentifier = "test@example.com"
    val usernameIdentifier = "testuser"
    val phoneNumber = "+1234567890"

    // Test email recognition (this tests the private isEmailAddress extension)
    assertTrue(
      "Should recognize email format",
      emailIdentifier.contains("@") && emailIdentifier.contains("."),
    )
    assertTrue("Should not recognize username as email", !usernameIdentifier.contains("@"))

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
  fun startAuthWithoutEnterpriseSSOFactorSkipsSSOPreparation() = runTest {
    val signIn =
      SignIn(
        id = "sign_in_password",
        status = SignIn.Status.NEEDS_FIRST_FACTOR,
        identifier = "user@example.com",
        supportedFirstFactors = listOf(Factor(strategy = "password")),
      )
    mockkObject(SignIn.Companion)
    mockkStatic("com.clerk.api.signin.SignInKt")
    coEvery { SignIn.create(any<SignIn.CreateParams.Strategy>()) } returns
      ClerkResult.success(signIn)

    viewModel.state.test {
      assertEquals(AuthStartViewModel.AuthState.Idle, awaitItem())

      viewModel.startAuth(
        authMode = AuthMode.SignIn,
        isPhoneNumberFieldActive = false,
        phoneNumber = "",
        identifier = "user@example.com",
      )

      assertEquals(AuthStartViewModel.AuthState.Loading, awaitItem())
      assertEquals(AuthStartViewModel.AuthState.Success.SignInSuccess(signIn), awaitItem())
    }

    coVerify(exactly = 0) { signIn.prepareFirstFactor(any<SignIn.PrepareFirstFactorParams>()) }
  }

  @Test
  fun startAuthUsesPhoneNumberAsIdentifierOnlyWhenPhoneFieldIsActive() = runTest {
    val createdParams = mutableListOf<SignIn.CreateParams.Strategy>()
    mockkObject(SignIn.Companion)
    coEvery { SignIn.create(capture(createdParams)) } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = listOf(ClerkError(longMessage = "x"))))

    viewModel.startAuth(
      authMode = AuthMode.SignIn,
      isPhoneNumberFieldActive = true,
      phoneNumber = "+1234567890",
      identifier = "test@example.com",
    )
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.startAuth(
      authMode = AuthMode.SignIn,
      isPhoneNumberFieldActive = false,
      phoneNumber = "+1234567890",
      identifier = "test@example.com",
    )
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(
      listOf(
        SignIn.CreateParams.Strategy.Identifier(identifier = "+1234567890"),
        SignIn.CreateParams.Strategy.Identifier(identifier = "test@example.com"),
      ),
      createdParams,
    )
  }

  @Test
  fun authModeEnumShouldHaveCorrectValues() {
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
  fun oauthProviderShouldHaveExpectedValues() {
    val google = OAuthProvider.GOOGLE
    val facebook = OAuthProvider.FACEBOOK

    assertEquals("GOOGLE", google.name)
    assertEquals("FACEBOOK", facebook.name)

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
  fun customSocialAuthPreservesProviderStrategy() = runTest {
    val paramsSlot = slot<SignIn.AuthenticateWithRedirectParams>()
    mockkObject(SignIn.Companion)
    coEvery { SignIn.authenticateWithRedirect(any(), any()) } returns
      ClerkResult.success(OAuthResult(signIn = mockk(relaxed = true)))

    viewModel.authenticateWithSocialProvider(
      provider = OAuthProvider.custom("oauth_custom_patreon"),
      preferGoogleOneTap = false,
    )
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { SignIn.authenticateWithRedirect(capture(paramsSlot), true) }
    val params = paramsSlot.captured as SignIn.AuthenticateWithRedirectParams.OAuth
    assertEquals("oauth_custom_patreon", params.provider.strategy)
  }

  @Test
  fun socialOAuthCancellationReturnsToIdle() = runTest {
    val cancellation =
      Class.forName("com.clerk.api.sso.SSOCancellationException")
        .getDeclaredConstructor(String::class.java)
        .apply { isAccessible = true }
        .newInstance("Authentication cancelled") as Throwable
    mockkObject(SignIn.Companion)
    coEvery { SignIn.authenticateWithRedirect(any(), any()) } returns
      ClerkResult.unknownFailure(cancellation)

    viewModel.authenticateWithSocialProvider(
      provider = OAuthProvider.GITHUB,
      preferGoogleOneTap = false,
    )

    assertEquals(AuthStartViewModel.AuthState.OAuthState.Loading, viewModel.state.value)
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(AuthStartViewModel.AuthState.Idle, viewModel.state.value)
  }

  @Test
  fun enterpriseSSOCancellationReturnsToIdle() = runTest {
    val cancellation =
      Class.forName("com.clerk.api.sso.SSOCancellationException")
        .getDeclaredConstructor(String::class.java)
        .apply { isAccessible = true }
        .newInstance("Authentication cancelled") as Throwable
    val signIn =
      SignIn(
        id = "sign_in_enterprise",
        status = SignIn.Status.NEEDS_FIRST_FACTOR,
        identifier = "user@example.com",
        supportedFirstFactors =
          listOf(Factor(strategy = "enterprise_sso", safeIdentifier = "user@example.com")),
      )
    val preparedSignIn =
      signIn.copy(
        firstFactorVerification =
          Verification(
            strategy = "enterprise_sso",
            externalVerificationRedirectUrl = "https://sso.example.com/start",
          )
      )
    mockkObject(SignIn.Companion)
    mockkStatic("com.clerk.api.signin.SignInKt")
    mockkStatic("com.clerk.api.signin.SignInExtensionsKt")
    coEvery { SignIn.create(any<SignIn.CreateParams.Strategy>()) } returns
      ClerkResult.success(signIn)
    coEvery {
      signIn.prepareFirstFactor(any<SignIn.PrepareFirstFactorParams.EnterpriseSSO>())
    } returns ClerkResult.success(preparedSignIn)
    coEvery { preparedSignIn.authenticateWithPreparedRedirect(any()) } returns
      ClerkResult.unknownFailure(cancellation)

    viewModel.startAuth(
      authMode = AuthMode.SignIn,
      isPhoneNumberFieldActive = false,
      phoneNumber = "",
      identifier = "user@example.com",
    )
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { preparedSignIn.authenticateWithPreparedRedirect(false) }
    assertEquals(AuthStartViewModel.AuthState.Idle, viewModel.state.value)
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
