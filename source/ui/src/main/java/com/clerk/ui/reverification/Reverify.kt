package com.clerk.ui.reverification

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.ReverificationConfig
import com.clerk.api.session.ReverificationRequiredException
import com.clerk.api.session.SessionVerification
import com.clerk.api.session.reverificationRequiredOrNull

/**
 * Runs [block], and when it fails because the action needs the user to verify their identity again,
 * shows Clerk's reverification screen and runs [block] once more after the user verifies.
 *
 * A failure asks for reverification when [reverificationRequiredOrNull] recognizes it: Clerk's
 * `session_reverification_required` API error, or a [ReverificationRequiredException] that your own
 * networking code returns for your backend's reverification response. The screen opens over the
 * activity your app shows, so you can call this from a ViewModel or any other code, with or without
 * Compose.
 *
 * ```kotlin
 * val result = Clerk.reverify { user.delete() }
 * if ((result as? ClerkResult.Failure)?.throwable is ReverificationCancelledException) {
 *   // The user closed the reverification screen.
 * }
 * ```
 *
 * @return The result of running [block] again after the user verifies. When the user closes the
 *   screen, or a different session becomes active before [block] runs again, a failure whose
 *   [ClerkResult.Failure.throwable] is a [ReverificationCancelledException]. Otherwise, the result
 *   of the first run: when it doesn't ask for reverification, when no one is signed in, or when
 *   your app shows no activity to open the screen over.
 */
public suspend fun <T : Any, E : Any> Clerk.reverify(
  block: suspend () -> ClerkResult<T, E>
): ClerkResult<T, E> = Reverifier.shared.run(block)

/**
 * The [ClerkResult.Failure.throwable] that [reverify] returns when the user closes the
 * reverification screen before verifying, or when a different session becomes active while it
 * shows.
 */
public class ReverificationCancelledException :
  Exception("The user didn't verify their identity, so the action didn't run again.")

internal data class SignedInIdentity(val clientId: String?, val sessionId: String?)

internal enum class ReverificationOutcome {
  VERIFIED,
  CANCELLED,
  NOT_PRESENTED,
}

internal class Reverifier(
  private val currentIdentity: () -> SignedInIdentity,
  private val present:
    suspend (SessionVerification.Level, SignedInIdentity) -> ReverificationOutcome,
) {
  suspend fun <T : Any, E : Any> run(block: suspend () -> ClerkResult<T, E>): ClerkResult<T, E> {
    val identity = currentIdentity()
    val first = block()
    val required = (first as? ClerkResult.Failure)?.reverificationRequiredOrNull()
    return when {
      required == null || identity.sessionId == null -> first
      currentIdentity() != identity -> cancelled()
      else ->
        when (present(required.level, identity)) {
          ReverificationOutcome.VERIFIED ->
            if (currentIdentity() == identity) block() else cancelled()
          ReverificationOutcome.CANCELLED -> cancelled()
          ReverificationOutcome.NOT_PRESENTED -> first
        }
    }
  }

  private fun cancelled(): ClerkResult.Failure<Nothing> =
    ClerkResult.unknownFailure(ReverificationCancelledException())

  companion object {
    val shared =
      Reverifier(
        currentIdentity = {
          SignedInIdentity(clientId = Clerk.clientFlow.value?.id, sessionId = Clerk.session?.id)
        },
        present = ReverificationPresenter.shared::present,
      )
  }
}

internal val ReverificationRequiredException.level: SessionVerification.Level
  get() =
    when (val requested = reverification) {
      ReverificationConfig.StrictMfa -> SessionVerification.Level.MULTI_FACTOR
      is ReverificationConfig.Custom ->
        requested.level.takeUnless { it == SessionVerification.Level.UNKNOWN }
          ?: SessionVerification.Level.SECOND_FACTOR
      else -> SessionVerification.Level.SECOND_FACTOR
    }
