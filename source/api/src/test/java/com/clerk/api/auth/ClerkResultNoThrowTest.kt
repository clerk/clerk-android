package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.auth.types.IdTokenProvider
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.reauthorize
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.OrganizationApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.revoke
import com.clerk.api.signin.FIRST_FACTOR_STRATEGY_NOT_SUPPORTED
import com.clerk.api.signin.SECOND_FACTOR_STRATEGY_NOT_SUPPORTED
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.prepareSecondFactor
import com.clerk.api.signin.sendCode
import com.clerk.api.signin.sendEmailCode
import com.clerk.api.signin.sendEmailLink
import com.clerk.api.signin.sendMfaEmailCode
import com.clerk.api.signin.sendMfaPhoneCode
import com.clerk.api.signin.sendPhoneCode
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.sendCode
import com.clerk.api.sso.OAuthProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ClerkResultNoThrowTest {
  private val signInApi = mockk<SignInApi>(relaxed = true)
  private val signUpApi = mockk<SignUpApi>(relaxed = true)
  private val organizationApi = mockk<OrganizationApi>(relaxed = true)
  private val userApi = mockk<UserApi>(relaxed = true)

  @Before
  fun setup() {
    Clerk.updateClient(Client())
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    every { ClerkApi.signUp } returns signUpApi
    every { ClerkApi.organization } returns organizationApi
    every { ClerkApi.user } returns userApi
  }

  @After
  fun tearDown() {
    unmockkAll()
    Clerk.updateClient(Client())
  }

  @Test
  fun `auth builders report missing or conflicting values as invalid arguments`() = runTest {
    val auth = Auth()

    val results =
      listOf(
        auth.signIn {},
        auth.signInWithPassword { identifier = "user@example.com" },
        auth.signInWithPassword { password = "secret" },
        auth.signInWithOtp {},
        auth.signInWithOtp {
          email = "user@example.com"
          phone = "+15555550123"
        },
        auth.signInWithIdToken { provider = IdTokenProvider.GOOGLE },
        auth.signInWithIdToken { token = "token" },
        auth.signInWithEnterpriseSso {},
        auth.signUpWithEnterpriseSso {},
      )

    results.forEach { assertErrorCode(LocalFailureCodes.INVALID_ARGUMENTS, it) }
    verify(exactly = 0) { ClerkApi.signIn }
  }

  @Test
  fun `send code builders report invalid channels as failures`() = runTest {
    val signIn = SignIn(id = "sign_in_123")
    val signUp = mockk<SignUp>(relaxed = true)

    assertErrorCode(LocalFailureCodes.INVALID_ARGUMENTS, signIn.sendCode {})
    assertErrorCode(
      LocalFailureCodes.INVALID_ARGUMENTS,
      signUp.sendCode {
        email = "user@example.com"
        phone = "+15555550123"
      },
    )
  }

  @Test
  fun `sign-in send helpers report missing factors as failures`() = runTest {
    val firstFactor = SignIn(id = "sign_in_123", status = SignIn.Status.NEEDS_FIRST_FACTOR)
    val secondFactor = SignIn(id = "sign_in_123", status = SignIn.Status.NEEDS_SECOND_FACTOR)

    assertErrorCode(FIRST_FACTOR_STRATEGY_NOT_SUPPORTED, firstFactor.sendPhoneCode())
    assertErrorCode(FIRST_FACTOR_STRATEGY_NOT_SUPPORTED, firstFactor.sendEmailCode())
    assertErrorCode(FIRST_FACTOR_STRATEGY_NOT_SUPPORTED, firstFactor.sendEmailLink())
    assertErrorCode(SECOND_FACTOR_STRATEGY_NOT_SUPPORTED, secondFactor.prepareSecondFactor())
    assertErrorCode(SECOND_FACTOR_STRATEGY_NOT_SUPPORTED, secondFactor.sendMfaPhoneCode())
    assertErrorCode(SECOND_FACTOR_STRATEGY_NOT_SUPPORTED, secondFactor.sendMfaEmailCode())
    verify(exactly = 0) { ClerkApi.signIn }
  }

  @Test
  fun `resource helpers report missing data as failures`() = runTest {
    val invitation =
      OrganizationInvitation(
        id = "orginv_123",
        emailAddress = "user@example.com",
        organizationId = null,
        publicMetadata = JsonNull,
        role = "org:member",
        status = OrganizationInvitation.Status.Pending,
        createdAt = 0,
        updatedAt = 0,
      )
    val externalAccount =
      ExternalAccount(
        id = "eac_123",
        identificationId = "idn_123",
        provider = "google",
        providerUserId = "provider_user",
        emailAddress = "user@example.com",
        approvedScopes = "",
        verification = null,
        createdAt = 0,
      )

    assertErrorCode(LocalFailureCodes.MISSING_RESOURCE_DATA, invitation.revoke())
    assertErrorCode(LocalFailureCodes.MISSING_RESOURCE_DATA, externalAccount.reauthorize())
    verify(exactly = 0) { ClerkApi.organization }
    verify(exactly = 0) { ClerkApi.user }
  }

  @Test
  fun `redirect sign-up without an external verification URL is a coded failure`() = runTest {
    val signUp = mockk<SignUp>(relaxed = true)
    every { signUp.verifications } returns emptyMap()
    coEvery { signUpApi.createSignUp(any()) } returns ClerkResult.success(signUp)

    val result = Auth().signUpWithOAuth(OAuthProvider.GOOGLE)

    assertEquals(ClerkResult.Failure.ErrorType.API, (result as ClerkResult.Failure).errorType)
    assertErrorCode(LocalFailureCodes.MISSING_RESOURCE_DATA, result)
  }

  private fun assertErrorCode(code: String, result: ClerkResult<*, ClerkErrorResponse>) {
    val failure = result as ClerkResult.Failure<ClerkErrorResponse>
    assertEquals(code, failure.error?.errors?.single()?.code)
  }
}
