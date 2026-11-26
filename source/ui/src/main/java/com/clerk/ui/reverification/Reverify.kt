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
 * Asks the user to verify their identity again, unless they already did within [reverification].
 *
 * When [com.clerk.api.session.Session.checkAuthorization] with [reverification] is `false`, it
 * shows Clerk's reverification screen over the activity your app shows, and returns once the user
 * verifies. Call it before a sensitive step, so the user verifies before your app sends the request
 * instead of after your backend rejects it.
 *
 * ```kotlin
 * if (Clerk.reverify(ReverificationConfig.Strict) is ClerkResult.Success) {
 *   showDeleteAccountForm()
 * }
 * ```
 *
 * @return Success when the user has verified within [reverification], before the call or on the
 *   screen. When the user closes the screen, or a different session becomes active while it shows,
 *   a failure whose [ClerkResult.Failure.throwable] is a [ReverificationCancelledException]. A
 *   failure with an [IllegalArgumentException] when [reverification] is a
 *   [ReverificationConfig.Custom] whose level is unknown or whose `afterMinutes` isn't positive,
 *   and with an [IllegalStateException] when no one is signed in or your app shows no activity to
 *   open the screen over.
 */
public suspend fun Clerk.reverify(
  reverification: ReverificationConfig
): ClerkResult<Unit, Nothing> = Reverifier.shared.verify(reverification)

/**
 * The [ClerkResult.Failure.throwable] that [reverify] returns when the user closes the
 * reverification screen before verifying, or when a different session becomes active while it
 * shows.
 */
public class ReverificationCancelledException :
  Exception("The user closed reverification before verifying their identity.")

internal val ClerkResult.Failure<*>.isReverificationCancelled: Boolean
  get() = throwable is ReverificationCancelledException

internal fun <T : Any, E : Any> withRetryFlag(
  block: suspend (isRetry: Boolean) -> ClerkResult<T, E>
): suspend () -> ClerkResult<T, E> {
  var runs = 0
  return { block(runs++ > 0) }
}

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
  private val isSatisfied: (ReverificationConfig) -> Boolean = { false },
) {
  suspend fun verify(reverification: ReverificationConfig): ClerkResult<Unit, Nothing> {
    val identity = currentIdentity()
    val level = reverification.levelOrNull
    return when {
      level == null -> ClerkResult.unknownFailure(IllegalArgumentException(UNSUPPORTED_MESSAGE))
      identity.sessionId == null ->
        ClerkResult.unknownFailure(IllegalStateException(NOT_SIGNED_IN_MESSAGE))
      isSatisfied(reverification) -> ClerkResult.success(Unit)
      else ->
        when (present(level, identity)) {
          ReverificationOutcome.VERIFIED ->
            if (currentIdentity() == identity) ClerkResult.success(Unit) else cancelled()
          ReverificationOutcome.CANCELLED -> cancelled()
          ReverificationOutcome.NOT_PRESENTED ->
            ClerkResult.unknownFailure(IllegalStateException(NOT_PRESENTED_MESSAGE))
        }
    }
  }

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
    private const val UNSUPPORTED_MESSAGE =
      "Clerk can't reverify with this requirement because its level or age isn't one Clerk supports."
    private const val NOT_SIGNED_IN_MESSAGE = "You need to be signed in to verify your identity."
    private const val NOT_PRESENTED_MESSAGE =
      "Clerk couldn't show reverification because the app shows no activity to open it over."

    val shared =
      Reverifier(
        currentIdentity = {
          SignedInIdentity(clientId = Clerk.clientFlow.value?.id, sessionId = Clerk.session?.id)
        },
        present = ReverificationPresenter.shared::present,
        isSatisfied = { Clerk.session?.checkAuthorization(reverification = it) == true },
      )
  }
}

internal val ReverificationRequiredException.level: SessionVerification.Level
  get() = reverification?.levelOrNull ?: SessionVerification.Level.SECOND_FACTOR

internal val ReverificationConfig.levelOrNull: SessionVerification.Level?
  get() =
    when (this) {
      ReverificationConfig.StrictMfa -> SessionVerification.Level.MULTI_FACTOR
      ReverificationConfig.Strict,
      ReverificationConfig.Moderate,
      ReverificationConfig.Lax -> SessionVerification.Level.SECOND_FACTOR
      is ReverificationConfig.Custom ->
        level.takeIf { it != SessionVerification.Level.UNKNOWN && afterMinutes > 0 }
    }
