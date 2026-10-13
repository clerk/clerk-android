package com.clerk.ui.protect

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.protect.ClerkProtect
import com.clerk.api.protect.ProtectCheckResource
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.reload
import com.clerk.api.signin.submitProtectCheck
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.get as reloadSignUp
import com.clerk.api.signup.submitProtectCheck
import kotlin.coroutines.cancellation.CancellationException

internal interface ProtectFlowOperations<T : Any> {
  fun protectCheck(resource: T): ProtectCheckResource?

  fun requiresProtectCheck(resource: T): Boolean

  suspend fun submit(resource: T, proofToken: String): ClerkResult<T, ClerkErrorResponse>

  suspend fun reload(resource: T): ClerkResult<T, ClerkErrorResponse>
}

internal object SignInProtectOperations : ProtectFlowOperations<SignIn> {
  override fun protectCheck(resource: SignIn): ProtectCheckResource? = resource.protectCheck

  override fun requiresProtectCheck(resource: SignIn): Boolean =
    resource.protectCheck != null || resource.status == SignIn.Status.NEEDS_PROTECT_CHECK

  override suspend fun submit(resource: SignIn, proofToken: String) =
    resource.submitProtectCheck(proofToken)

  override suspend fun reload(resource: SignIn) = resource.reload()
}

internal object SignUpProtectOperations : ProtectFlowOperations<SignUp> {
  override fun protectCheck(resource: SignUp): ProtectCheckResource? = resource.protectCheck

  override fun requiresProtectCheck(resource: SignUp): Boolean =
    resource.protectCheck != null || PROTECT_CHECK_FIELD in resource.missingFields

  override suspend fun submit(resource: SignUp, proofToken: String) =
    resource.submitProtectCheck(proofToken)

  override suspend fun reload(resource: SignUp) = resource.reloadSignUp()
}

internal class ProtectCheckResolver<T : Any>(
  private val operations: ProtectFlowOperations<T>,
  private val runChallenge: suspend (ProtectCheckResource) -> String,
  private val isExpired: (Throwable) -> Boolean = {
    ClerkProtect.errorCode(it) == PROTECT_CHECK_EXPIRED
  },
) {

  @Suppress("ReturnCount")
  suspend fun resolve(initial: T): ProtectResolution<T> {
    var current = initial
    var reloads = 0
    var challenges = 0
    while (operations.requiresProtectCheck(current)) {
      if (challenges >= MAX_CHALLENGES) return ProtectResolution.Failure()
      val check = operations.protectCheck(current) ?: return ProtectResolution.Failure()
      challenges += 1
      val proof =
        try {
          runChallenge(check)
        } catch (cancellation: CancellationException) {
          throw cancellation
        } catch (throwable: Throwable) {
          if (!isExpired(throwable) || reloads >= MAX_RELOADS) return ProtectResolution.Failure()
          reloads += 1
          current =
            reloadOrElse(current) {
              return it
            }
          continue
        }

      when (val submitted = operations.submit(current, proof)) {
        is ClerkResult.Success -> current = submitted.value
        is ClerkResult.Failure -> {
          if (!submitted.isAlreadyResolved() || reloads >= MAX_RELOADS) {
            return ProtectResolution.Failure(submitted.errorMessage)
          }
          reloads += 1
          current =
            reloadOrElse(current) {
              return it
            }
        }
      }
    }
    return ProtectResolution.Success(current)
  }

  private suspend inline fun reloadOrElse(
    resource: T,
    onFailure: (ProtectResolution.Failure) -> Nothing,
  ): T =
    when (val refreshed = operations.reload(resource)) {
      is ClerkResult.Success -> refreshed.value
      is ClerkResult.Failure -> onFailure(ProtectResolution.Failure(refreshed.errorMessage))
    }

  internal companion object {
    const val MAX_CHALLENGES = 5
    const val MAX_RELOADS = 2
  }
}

internal sealed interface ProtectResolution<out T : Any> {
  data class Success<T : Any>(val resource: T) : ProtectResolution<T>

  data class Failure(val message: String? = null) : ProtectResolution<Nothing>
}

private fun ClerkResult.Failure<ClerkErrorResponse>.isAlreadyResolved(): Boolean =
  error?.errors?.firstOrNull()?.code == PROTECT_CHECK_ALREADY_RESOLVED

private const val PROTECT_CHECK_ALREADY_RESOLVED = "protect_check_already_resolved"
private const val PROTECT_CHECK_EXPIRED = "protect_check_expired"
private const val PROTECT_CHECK_FIELD = "protect_check"
