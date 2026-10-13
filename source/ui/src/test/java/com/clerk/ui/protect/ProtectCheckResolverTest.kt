package com.clerk.ui.protect

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.protect.ProtectCheckResource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectCheckResolverTest {

  @Test
  fun `resolves once the server stops returning a protect check`() = runTest {
    val operations = FakeOperations(submitResults = listOf(Attempt(check = check()), Attempt()))
    var challenges = 0

    val result =
      ProtectCheckResolver(
          operations,
          runChallenge = {
            challenges++
            "proof"
          },
        )
        .resolve(Attempt(check = check()))

    assertEquals(ProtectResolution.Success(Attempt()), result)
    assertEquals(2, challenges)
  }

  @Test
  fun `stops with a failure when the server keeps chaining protect checks`() = runTest {
    val operations = FakeOperations(submitResults = List(10) { Attempt(check = check()) })
    var challenges = 0

    val result =
      ProtectCheckResolver(
          operations,
          runChallenge = {
            challenges++
            "proof"
          },
        )
        .resolve(Attempt(check = check()))

    assertTrue(result is ProtectResolution.Failure)
    assertEquals(ProtectCheckResolver.MAX_CHALLENGES, challenges)
  }

  @Test
  fun `reloads an expired challenge a bounded number of times`() = runTest {
    val operations = FakeOperations(reloadResult = Attempt(check = check()))

    val result =
      ProtectCheckResolver<Attempt>(
          operations,
          runChallenge = { throw IllegalStateException("expired") },
          isExpired = { true },
        )
        .resolve(Attempt(check = check()))

    assertTrue(result is ProtectResolution.Failure)
    assertEquals(ProtectCheckResolver.MAX_RELOADS, operations.reloads)
  }

  @Test
  fun `already resolved submit reloads the resource`() = runTest {
    val alreadyResolved =
      ClerkResult.apiFailure(
        ClerkErrorResponse(errors = listOf(Error(code = "protect_check_already_resolved")))
      )
    val operations =
      FakeOperations(submitFailure = alreadyResolved, reloadResult = Attempt(check = null))

    val result =
      ProtectCheckResolver(operations, runChallenge = { "proof" }).resolve(Attempt(check = check()))

    assertEquals(ProtectResolution.Success(Attempt()), result)
    assertEquals(1, operations.reloads)
  }

  private data class Attempt(val check: ProtectCheckResource? = null)

  private class FakeOperations(
    private val submitResults: List<Attempt> = emptyList(),
    private val submitFailure: ClerkResult.Failure<ClerkErrorResponse>? = null,
    private val reloadResult: Attempt = Attempt(),
  ) : ProtectFlowOperations<Attempt> {
    private var submits = 0
    var reloads = 0
      private set

    override fun protectCheck(resource: Attempt): ProtectCheckResource? = resource.check

    override fun requiresProtectCheck(resource: Attempt): Boolean = resource.check != null

    override suspend fun submit(
      resource: Attempt,
      proofToken: String,
    ): ClerkResult<Attempt, ClerkErrorResponse> =
      submitFailure ?: ClerkResult.success(submitResults[submits++])

    override suspend fun reload(resource: Attempt): ClerkResult<Attempt, ClerkErrorResponse> {
      reloads += 1
      return ClerkResult.success(reloadResult)
    }
  }

  private fun check(): ProtectCheckResource =
    Json.decodeFromString(
      """{"status": "pending", "token": "t", "sdk_url": "https://specter.example.com/c"}"""
    )
}
