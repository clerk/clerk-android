package com.clerk.ui.reverification

import android.content.ActivityNotFoundException
import com.clerk.api.Clerk
import com.clerk.api.session.SessionVerification
import com.clerk.ui.core.log.ClerkLog
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ReverificationRequest(
  val id: String,
  val level: SessionVerification.Level,
  val identity: SignedInIdentity,
) {
  internal val outcome = CompletableDeferred<ReverificationOutcome>()
  internal var waiters = 0

  val isFinished: Boolean
    get() = outcome.isCompleted

  suspend fun awaitEnd() {
    outcome.join()
  }
}

internal fun interface ReverificationDisplay {
  suspend fun show(request: ReverificationRequest): Boolean
}

internal class ReverificationPresenter(private val display: ReverificationDisplay) {
  private val lock = Any()
  private var active: ReverificationRequest? = null

  fun request(id: String): ReverificationRequest? =
    synchronized(lock) { active?.takeIf { it.id == id } }

  suspend fun present(
    level: SessionVerification.Level,
    identity: SignedInIdentity,
  ): ReverificationOutcome {
    while (true) {
      when (val step = nextStep(level, identity)) {
        is Step.Wait -> step.request.awaitEnd()
        is Step.Join -> return awaitOutcome(step.request)
        is Step.Show -> {
          if (!display.show(step.request)) end(step.request, ReverificationOutcome.NOT_PRESENTED)
          return awaitOutcome(step.request)
        }
      }
    }
  }

  fun complete(request: ReverificationRequest, verifiedSessionId: String?) {
    val outcome =
      if (request.identity.sessionId == verifiedSessionId) {
        ReverificationOutcome.VERIFIED
      } else {
        ReverificationOutcome.CANCELLED
      }
    end(request, outcome)
  }

  fun dismiss(request: ReverificationRequest) {
    end(request, ReverificationOutcome.CANCELLED)
  }

  private fun nextStep(level: SessionVerification.Level, identity: SignedInIdentity): Step =
    synchronized(lock) {
      val current = active
      when {
        current == null ->
          ReverificationRequest(UUID.randomUUID().toString(), level, identity)
            .also {
              it.waiters = 1
              active = it
            }
            .let(Step::Show)
        current.level == level && current.identity == identity -> {
          current.waiters += 1
          Step.Join(current)
        }
        else -> Step.Wait(current)
      }
    }

  private suspend fun awaitOutcome(request: ReverificationRequest): ReverificationOutcome =
    try {
      request.outcome.await()
    } finally {
      val abandoned =
        synchronized(lock) {
          request.waiters -= 1
          request.waiters == 0 && !request.isFinished
        }
      if (abandoned) end(request, ReverificationOutcome.CANCELLED)
    }

  private fun end(request: ReverificationRequest, outcome: ReverificationOutcome) {
    synchronized(lock) {
      if (active === request) active = null
    }
    request.outcome.complete(outcome)
  }

  private sealed interface Step {
    val request: ReverificationRequest

    data class Show(override val request: ReverificationRequest) : Step

    data class Join(override val request: ReverificationRequest) : Step

    data class Wait(override val request: ReverificationRequest) : Step
  }

  companion object {
    val shared = ReverificationPresenter(ActivityReverificationDisplay)
  }
}

internal object ActivityReverificationDisplay : ReverificationDisplay {
  override suspend fun show(request: ReverificationRequest): Boolean =
    withContext(Dispatchers.Main.immediate) {
      val activity =
        Clerk.foregroundActivity?.takeUnless { it.isFinishing || it.isDestroyed }
          ?: return@withContext false.also {
            ClerkLog.w("Couldn't show the reverification screen: the app shows no activity.")
          }
      try {
        activity.startActivity(ReverificationActivity.intent(activity, request.id))
        true
      } catch (e: ActivityNotFoundException) {
        ClerkLog.w("Couldn't show the reverification screen: ${e.message}")
        false
      }
    }
}
