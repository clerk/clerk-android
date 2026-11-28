package com.clerk.ui.reverification

import com.clerk.api.session.SessionVerification
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReverificationPresenterTest {
  private val identity = SignedInIdentity(clientId = "client_1", sessionId = "sess_1")
  private val shown = mutableListOf<ReverificationRequest>()
  private var canShow = true
  private val presenter =
    ReverificationPresenter(display = { request -> canShow.also { if (it) shown += request } })

  private fun TestScope.presentAsync(
    level: SessionVerification.Level = SessionVerification.Level.SECOND_FACTOR,
    identity: SignedInIdentity = this@ReverificationPresenterTest.identity,
  ) = async { presenter.present(level, identity) }.also { runCurrent() }

  @Test
  fun verifyingTheSignedInSessionReturnsVerified() = runTest {
    val caller = presentAsync()

    presenter.complete(shown.single(), verifiedSessionId = "sess_1")

    assertEquals(ReverificationOutcome.VERIFIED, caller.await())
    assertNull(presenter.request(shown.single().id))
  }

  @Test
  fun verifyingAnotherSessionReturnsCancelled() = runTest {
    val caller = presentAsync()

    presenter.complete(shown.single(), verifiedSessionId = "sess_2")

    assertEquals(ReverificationOutcome.CANCELLED, caller.await())
  }

  @Test
  fun closingTheScreenReturnsCancelled() = runTest {
    val caller = presentAsync()

    presenter.dismiss(shown.single())

    assertEquals(ReverificationOutcome.CANCELLED, caller.await())
  }

  @Test
  fun closingAfterVerifyingKeepsVerified() = runTest {
    val caller = presentAsync()

    presenter.complete(shown.single(), verifiedSessionId = "sess_1")
    presenter.dismiss(shown.single())

    assertEquals(ReverificationOutcome.VERIFIED, caller.await())
  }

  @Test
  fun aScreenThatCannotShowReturnsNotPresentedAndFreesTheSlot() = runTest {
    canShow = false
    assertEquals(ReverificationOutcome.NOT_PRESENTED, presentAsync().await())

    canShow = true
    val retry = presentAsync()
    assertEquals(1, shown.size)
    presenter.dismiss(shown.single())
    retry.await()
  }

  @Test
  fun callersAtTheSameLevelForTheSameIdentityShareOneScreen() = runTest {
    val first = presentAsync()
    val second = presentAsync()

    assertEquals(1, shown.size)
    presenter.complete(shown.single(), verifiedSessionId = "sess_1")

    assertEquals(ReverificationOutcome.VERIFIED, first.await())
    assertEquals(ReverificationOutcome.VERIFIED, second.await())
  }

  @Test
  fun aCallerAtAnotherLevelWaitsForTheScreenThenShowsItsOwn() = runTest {
    val first = presentAsync(SessionVerification.Level.SECOND_FACTOR)
    val second = presentAsync(SessionVerification.Level.MULTI_FACTOR)
    assertEquals(1, shown.size)

    presenter.dismiss(shown[0])
    runCurrent()

    assertEquals(ReverificationOutcome.CANCELLED, first.await())
    assertEquals(2, shown.size)
    assertEquals(SessionVerification.Level.MULTI_FACTOR, shown[1].level)
    presenter.complete(shown[1], verifiedSessionId = "sess_1")
    assertEquals(ReverificationOutcome.VERIFIED, second.await())
  }

  @Test
  fun aCallerForAnotherIdentityDoesNotJoinTheScreen() = runTest {
    presentAsync()
    val other = presentAsync(identity = identity.copy(sessionId = "sess_2"))
    assertEquals(1, shown.size)

    presenter.dismiss(shown[0])
    runCurrent()

    assertEquals(2, shown.size)
    assertEquals("sess_2", shown[1].identity.sessionId)
    assertFalse(other.isCompleted)
    presenter.dismiss(shown[1])
    assertEquals(ReverificationOutcome.CANCELLED, other.await())
  }

  @Test
  fun cancellingEveryCallerEndsTheScreenAsCancelled() = runTest {
    val caller = presentAsync()
    val request = shown.single()

    caller.cancel()
    runCurrent()

    assertTrue(request.isFinished)
    assertEquals(ReverificationOutcome.CANCELLED, request.outcome.await())
    assertNull(presenter.request(request.id))
  }

  @Test
  fun cancellingOneOfTwoCallersKeepsTheScreenForTheOther() = runTest {
    val first = presentAsync()
    val second = presentAsync()
    val request = shown.single()

    first.cancel()
    runCurrent()

    assertFalse(request.isFinished)
    assertSame(request, presenter.request(request.id))
    presenter.complete(request, verifiedSessionId = "sess_1")
    assertEquals(ReverificationOutcome.VERIFIED, second.await())
  }
}
