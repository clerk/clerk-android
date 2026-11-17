package com.clerk.api.sessiontransfer

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error as ClerkError
import com.clerk.api.network.model.sessiontransfer.SessionTransferResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.session.Session
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionTransferServiceTest {
  private val sessionApi = mockk<SessionApi>()

  @Before
  fun setup() {
    mockkObject(Clerk)
    every { Clerk.activeSession } returns mockk<Session> { every { id } returns SESSION_ID }
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun prepareMapsTheMintedUrlAndExpiry() = runBlocking {
    stubCreate(ClerkResult.success(resource()))

    val result = SessionTransferService.prepare(DESTINATION, SessionTransferPresentation.BROWSER)

    val transfer = (result as ClerkResult.Success).value
    assertEquals(TRANSFER_URL, transfer.url.toString())
    assertEquals(EXPIRES_AT, transfer.expiresAt)
    coVerify(exactly = 1) {
      sessionApi.createSessionTransfer(SESSION_ID, DESTINATION, "browser", any())
    }
  }

  @Test
  fun prepareSendsTheWebViewPresentationMode() = runBlocking {
    stubCreate(ClerkResult.success(resource()))

    SessionTransferService.prepare(DESTINATION, SessionTransferPresentation.WEB_VIEW)

    coVerify(exactly = 1) {
      sessionApi.createSessionTransfer(SESSION_ID, DESTINATION, "webview", any())
    }
  }

  @Test
  fun prepareRejectsAnUnexpectedObjectType() = runBlocking {
    assertInvalidResponse(resource(objectType = "hosted_auth"))
  }

  @Test
  fun prepareRejectsAnHttpUrl() = runBlocking {
    assertInvalidResponse(resource(url = "http://clerk.example.com/v1/session_transfers/redeem"))
  }

  @Test
  fun prepareRejectsAUrlWithUserInfo() = runBlocking {
    assertInvalidResponse(
      resource(url = "https://user:pass@clerk.example.com/v1/session_transfers/redeem")
    )
  }

  @Test
  fun prepareRejectsAUrlWithoutAHost() = runBlocking {
    assertInvalidResponse(resource(url = "https:///v1/session_transfers/redeem"))
  }

  @Test
  fun prepareRejectsAnOpaqueUrl() = runBlocking {
    assertInvalidResponse(resource(url = "https:clerk.example.com"))
  }

  @Test
  fun preparePassesFapiFailuresThrough() = runBlocking {
    val failure =
      ClerkResult.apiFailure(
        ClerkErrorResponse(
          errors =
            listOf(
              ClerkError(
                message = "not enabled",
                longMessage = "Session transfers are not enabled.",
                code = "feature_not_enabled",
              )
            )
        )
      )
    stubCreate(failure)

    val result = SessionTransferService.prepare(DESTINATION, SessionTransferPresentation.BROWSER)

    assertSame(failure, result)
    assertEquals(
      "feature_not_enabled",
      (result as ClerkResult.Failure).error?.errors?.single()?.code,
    )
  }

  @Test
  fun prepareFailsWithoutAnActiveSessionAndSkipsTheNetwork() = runBlocking {
    every { Clerk.activeSession } returns null

    assertLocalFailure(DESTINATION, LocalFailureCodes.SIGNED_OUT)
  }

  @Test
  fun prepareTreatsAPendingSessionAsSignedOutAndSkipsTheNetwork() = runBlocking {
    every { Clerk.session } returns
      mockk<Session> {
        every { id } returns SESSION_ID
        every { status } returns Session.SessionStatus.PENDING
      }
    every { Clerk.activeSession } returns null

    assertLocalFailure(DESTINATION, LocalFailureCodes.SIGNED_OUT)
  }

  @Test
  fun prepareRejectsADestinationThatIsNotAnAbsoluteWebUrlAndSkipsTheNetwork() = runBlocking {
    listOf(
        "myapp://disputes/new",
        "/disputes/new",
        "example.com/disputes/new",
        "https:///disputes/new",
        "https:example.com",
        "",
      )
      .forEach { destination ->
        assertLocalFailure(destination, LocalFailureCodes.INVALID_ARGUMENTS)
      }
  }

  @Test
  fun prepareAcceptsAnHttpDestination() = runBlocking {
    stubCreate(ClerkResult.success(resource()))

    val result =
      SessionTransferService.prepare(
        "http://localhost:3000/disputes/new",
        SessionTransferPresentation.BROWSER,
      )

    assertTrue(result is ClerkResult.Success)
  }

  @Test
  fun resourceToStringOmitsTheUrl() {
    val description = resource().toString()

    assertFalse(description.contains(TRANSFER_URL))
    assertFalse(description.contains("token="))
  }

  @Test
  fun toStringOmitsTheUrl() = runBlocking {
    stubCreate(ClerkResult.success(resource()))

    val transfer =
      (SessionTransferService.prepare(DESTINATION, SessionTransferPresentation.BROWSER)
          as ClerkResult.Success)
        .value

    val description = transfer.toString()
    assertFalse(description.contains(TRANSFER_URL))
    assertFalse(description.contains("token="))
    assertTrue(description.contains(EXPIRES_AT.toString()))
  }

  private suspend fun assertLocalFailure(destination: String, code: String) {
    val result = SessionTransferService.prepare(destination, SessionTransferPresentation.BROWSER)

    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.API, failure.errorType)
    assertEquals(code, failure.error?.errors?.single()?.code)
    coVerify(exactly = 0) { sessionApi.createSessionTransfer(any(), any(), any(), any()) }
  }

  private suspend fun assertInvalidResponse(resource: SessionTransferResource) {
    stubCreate(ClerkResult.success(resource))

    val result = SessionTransferService.prepare(DESTINATION, SessionTransferPresentation.BROWSER)

    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertTrue(failure.throwable is IllegalStateException)
    assertEquals(
      "Session transfer creation returned an invalid response.",
      failure.throwable?.message,
    )
    assertFalse(failure.throwable?.message.orEmpty().contains(resource.url))
  }

  private fun stubCreate(result: ClerkResult<SessionTransferResource, ClerkErrorResponse>) {
    coEvery { sessionApi.createSessionTransfer(any(), any(), any(), any()) } returns result
  }

  private fun resource(
    objectType: String = "session_transfer",
    url: String = TRANSFER_URL,
  ): SessionTransferResource =
    SessionTransferResource(objectType = objectType, url = url, expiresAt = EXPIRES_AT)

  private companion object {
    const val SESSION_ID = "sess_123"
    const val DESTINATION = "https://example.com/disputes/new?transaction=streamly"
    const val TRANSFER_URL = "https://clerk.example.com/v1/session_transfers/redeem?token=abc"
    const val EXPIRES_AT = 1_700_000_060_000L
  }
}
