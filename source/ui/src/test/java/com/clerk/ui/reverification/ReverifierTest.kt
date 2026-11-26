package com.clerk.ui.reverification

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.ReverificationConfig
import com.clerk.api.session.ReverificationRequiredException
import com.clerk.api.session.SessionVerification
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReverifierTest {
  private val signedIn = SignedInIdentity(clientId = "client_1", sessionId = "sess_1")
  private var identity = signedIn
  private val presented = mutableListOf<Pair<SessionVerification.Level, SignedInIdentity>>()
  private var outcome = ReverificationOutcome.VERIFIED
  private var duringPresent: () -> Unit = {}

  private val reverifier =
    Reverifier(
      currentIdentity = { identity },
      present = { level, identity ->
        presented += level to identity
        duringPresent()
        outcome
      },
    )

  private val apiReverificationRequired =
    ClerkResult.apiFailure(
      ClerkErrorResponse(errors = listOf(Error(code = "session_reverification_required")))
    )

  @Test
  fun aSuccessReturnsWithoutPresenting() = runTest {
    var runs = 0
    val result = reverifier.run { ClerkResult.success("done").also { runs++ } }

    assertEquals("done", (result as ClerkResult.Success).value)
    assertEquals(1, runs)
    assertTrue(presented.isEmpty())
  }

  @Test
  fun aFailureThatDoesNotAskForReverificationReturnsUnchanged() = runTest {
    val failure =
      ClerkResult.apiFailure(
        ClerkErrorResponse(errors = listOf(Error(code = "form_code_incorrect")))
      )

    val result = reverifier.run { failure }

    assertSame(failure, result)
    assertTrue(presented.isEmpty())
  }

  @Test
  fun aVerifiedPresentationRunsTheBlockOnceMore() = runTest {
    var runs = 0
    val result = reverifier.run {
      runs++
      if (runs == 1) apiReverificationRequired else ClerkResult.success("retried")
    }

    assertEquals("retried", (result as ClerkResult.Success).value)
    assertEquals(2, runs)
    assertEquals(listOf(SessionVerification.Level.SECOND_FACTOR to signedIn), presented)
  }

  @Test
  fun theRetryRunsOnlyOnceEvenWhenItAsksForReverificationAgain() = runTest {
    var runs = 0
    val result = reverifier.run { apiReverificationRequired.also { runs++ } }

    assertSame(apiReverificationRequired, result)
    assertEquals(2, runs)
    assertEquals(1, presented.size)
  }

  @Test
  fun aClosedScreenReturnsACancelledFailureWithoutRetrying() = runTest {
    outcome = ReverificationOutcome.CANCELLED
    var runs = 0

    val result = reverifier.run { apiReverificationRequired.also { runs++ } }

    assertTrue((result as ClerkResult.Failure).throwable is ReverificationCancelledException)
    assertEquals(1, runs)
  }

  @Test
  fun aScreenThatCannotShowReturnsTheOriginalFailure() = runTest {
    outcome = ReverificationOutcome.NOT_PRESENTED
    var runs = 0

    val result = reverifier.run { apiReverificationRequired.also { runs++ } }

    assertSame(apiReverificationRequired, result)
    assertEquals(1, runs)
  }

  @Test
  fun noSignedInSessionReturnsTheOriginalFailureWithoutPresenting() = runTest {
    identity = SignedInIdentity(clientId = "client_1", sessionId = null)

    val result = reverifier.run { apiReverificationRequired }

    assertSame(apiReverificationRequired, result)
    assertTrue(presented.isEmpty())
  }

  @Test
  fun aSessionChangeWhileTheScreenShowsCancelsWithoutRetrying() = runTest {
    duringPresent = { identity = signedIn.copy(sessionId = "sess_2") }
    var runs = 0

    val result = reverifier.run { apiReverificationRequired.also { runs++ } }

    assertTrue((result as ClerkResult.Failure).throwable is ReverificationCancelledException)
    assertEquals(1, runs)
  }

  @Test
  fun aSessionChangeDuringTheFirstRunCancelsWithoutPresenting() = runTest {
    val result = reverifier.run {
      identity = signedIn.copy(clientId = "client_2")
      apiReverificationRequired
    }

    assertTrue((result as ClerkResult.Failure).throwable is ReverificationCancelledException)
    assertTrue(presented.isEmpty())
  }

  @Test
  fun anAppReportedExceptionPresentsAtTheLevelItAsksFor() = runTest {
    var runs = 0
    reverifier.run {
      runs++
      if (runs == 1) {
        ClerkResult.unknownFailure(ReverificationRequiredException(ReverificationConfig.StrictMfa))
      } else {
        ClerkResult.success(Unit)
      }
    }

    assertEquals(SessionVerification.Level.MULTI_FACTOR, presented.single().first)
  }

  @Test
  fun eachRequirementMapsToTheLevelItVerifies() {
    val levels =
      listOf(
          null,
          ReverificationConfig.StrictMfa,
          ReverificationConfig.Strict,
          ReverificationConfig.Moderate,
          ReverificationConfig.Lax,
          ReverificationConfig.Custom(SessionVerification.Level.FIRST_FACTOR, 5),
          ReverificationConfig.Custom(SessionVerification.Level.UNKNOWN, 5),
        )
        .map { ReverificationRequiredException(it).level }

    assertEquals(
      listOf(
        SessionVerification.Level.SECOND_FACTOR,
        SessionVerification.Level.MULTI_FACTOR,
        SessionVerification.Level.SECOND_FACTOR,
        SessionVerification.Level.SECOND_FACTOR,
        SessionVerification.Level.SECOND_FACTOR,
        SessionVerification.Level.FIRST_FACTOR,
        SessionVerification.Level.SECOND_FACTOR,
      ),
      levels,
    )
  }
}
