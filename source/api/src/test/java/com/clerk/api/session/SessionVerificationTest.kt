package com.clerk.api.session

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.PasskeyService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionVerificationTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `session verification decodes supported factor metadata`() {
    val json =
      """
      {
        "id": "ver_123",
        "status": "needs_first_factor",
        "level": "first_factor",
        "supported_first_factors": [
          {
            "strategy": "enterprise_sso",
            "enterprise_connection_id": "econn_123",
            "enterprise_connection_name": "Acme"
          }
        ],
        "supported_second_factors": [
          {
            "strategy": "phone_code",
            "phone_number_id": "idn_123",
            "safe_identifier": "+15555550123",
            "primary": true,
            "default": true
          }
        ]
      }
      """
        .trimIndent()

    val verification = ClerkApi.json.decodeFromString<SessionVerification>(json)
    val firstFactor = verification.supportedFirstFactors!!.first()
    val secondFactor = verification.supportedSecondFactors!!.first()

    assertEquals(SessionVerification.Status.NEEDS_FIRST_FACTOR, verification.status)
    assertEquals(SessionVerification.Level.FIRST_FACTOR, verification.level)
    assertEquals("enterprise_sso", firstFactor.strategy)
    assertEquals("econn_123", firstFactor.enterpriseConnectionId)
    assertEquals("Acme", firstFactor.enterpriseConnectionName)
    assertEquals("phone_code", secondFactor.strategy)
    assertEquals("idn_123", secondFactor.phoneNumberId)
    assertEquals("+15555550123", secondFactor.safeIdentifier)
    assertEquals(true, secondFactor.primary)
    assertEquals(true, secondFactor.default)
  }

  @Test
  fun `start verification forwards level to session api`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.NEEDS_FIRST_FACTOR)
    val params = mapOf("level" to "first_factor")
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.startVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result = session.startVerification(SessionVerification.Level.FIRST_FACTOR)

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.startVerification("sess_123", params) }
  }

  @Test
  fun `send email code prepares email first factor`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.NEEDS_FIRST_FACTOR)
    val params = mapOf("strategy" to "email_code", "email_address_id" to "idn_email")
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.prepareFirstFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result = session.sendEmailCode(emailAddressId = "idn_email")

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.prepareFirstFactorVerification("sess_123", params) }
  }

  @Test
  fun `send phone code prepares phone first factor`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.NEEDS_FIRST_FACTOR)
    val params = mapOf("strategy" to "phone_code", "phone_number_id" to "idn_phone")
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.prepareFirstFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result = session.sendPhoneCode(phoneNumberId = "idn_phone")

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.prepareFirstFactorVerification("sess_123", params) }
  }

  @Test
  fun `verify with email code attempts first factor`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.COMPLETE)
    val params = mapOf("strategy" to "email_code", "code" to "123456")
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.attemptFirstFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result = session.verifyWithEmailCode("123456")

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.attemptFirstFactorVerification("sess_123", params) }
  }

  @Test
  fun `verify with phone code attempts first factor`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.COMPLETE)
    val params = mapOf("strategy" to "phone_code", "code" to "123456")
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.attemptFirstFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result = session.verifyWithPhoneCode("123456")

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.attemptFirstFactorVerification("sess_123", params) }
  }

  @Test
  fun `start enterprise sso prepares first factor`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.NEEDS_FIRST_FACTOR)
    val params =
      mapOf(
        "strategy" to "enterprise_sso",
        "email_address_id" to "idn_email",
        "enterprise_connection_id" to "econn_123",
        "redirect_url" to "clerk://callback",
      )
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.prepareFirstFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result =
      session.startEnterpriseSso(
        emailAddressId = "idn_email",
        enterpriseConnectionId = "econn_123",
        redirectUrl = "clerk://callback",
      )

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.prepareFirstFactorVerification("sess_123", params) }
  }

  @Test
  fun `start enterprise sso uses default redirect url`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.NEEDS_FIRST_FACTOR)
    val originalApplicationId = Clerk.applicationId
    Clerk.applicationId = "com.clerk.test"
    val params =
      mapOf(
        "strategy" to "enterprise_sso",
        "email_address_id" to "idn_email",
        "enterprise_connection_id" to "econn_123",
        "redirect_url" to "clerk://com.clerk.test.callback",
      )
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.prepareFirstFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    try {
      val result =
        session.startEnterpriseSso(
          emailAddressId = "idn_email",
          enterpriseConnectionId = "econn_123",
        )

      assertTrue(result is ClerkResult.Success)
      assertSame(verification, (result as ClerkResult.Success).value)
      coVerify(exactly = 1) { sessionApi.prepareFirstFactorVerification("sess_123", params) }
    } finally {
      Clerk.applicationId = originalApplicationId
    }
  }

  @Test
  fun `verify with password attempts first factor`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.COMPLETE)
    val params = mapOf("password" to "hunter2", "strategy" to "password")
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.attemptFirstFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result = session.verifyWithPassword("hunter2")

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.attemptFirstFactorVerification("sess_123", params) }
  }

  @Test
  fun `send mfa phone code prepares second factor`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.NEEDS_SECOND_FACTOR)
    val params = mapOf("strategy" to "phone_code", "phone_number_id" to "idn_phone")
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.prepareSecondFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result = session.sendMfaPhoneCode(phoneNumberId = "idn_phone")

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.prepareSecondFactorVerification("sess_123", params) }
  }

  @Test
  fun `verify with mfa phone code attempts second factor`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.COMPLETE)
    val params = mapOf("strategy" to "phone_code", "code" to "123456")
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.attemptSecondFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result = session.verifyWithMfaPhoneCode("123456")

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.attemptSecondFactorVerification("sess_123", params) }
  }

  @Test
  fun `verify with passkey delegates to passkey service`() = runTest {
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.COMPLETE)
    val allowedCredentialIds = listOf("credential_123")
    mockkObject(PasskeyService)
    coEvery { PasskeyService.verifySessionWithPasskey(session, allowedCredentialIds) } returns
      ClerkResult.success(verification)

    val result = session.verifyWithPasskey(allowedCredentialIds = allowedCredentialIds)

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { PasskeyService.verifySessionWithPasskey(session, allowedCredentialIds) }
  }

  @Test
  fun `verify with totp attempts second factor`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.COMPLETE)
    val params = mapOf("strategy" to "totp", "code" to "123456")
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.attemptSecondFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result = session.verifyWithTOTP("123456")

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.attemptSecondFactorVerification("sess_123", params) }
  }

  @Test
  fun `verify with backup code attempts second factor`() = runTest {
    val sessionApi = mockk<SessionApi>()
    val session = testSession()
    val verification = testVerification(status = SessionVerification.Status.COMPLETE)
    val params = mapOf("strategy" to "backup_code", "code" to "abcdef")
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.attemptSecondFactorVerification("sess_123", params) } returns
      ClerkResult.success(verification)

    val result = session.verifyWithBackupCode("abcdef")

    assertTrue(result is ClerkResult.Success)
    assertSame(verification, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { sessionApi.attemptSecondFactorVerification("sess_123", params) }
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

  private fun testVerification(status: SessionVerification.Status): SessionVerification {
    return SessionVerification(
      id = "ver_123",
      status = status,
      level = SessionVerification.Level.FIRST_FACTOR,
    )
  }
}
