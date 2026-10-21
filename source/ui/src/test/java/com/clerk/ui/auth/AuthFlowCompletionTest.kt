package com.clerk.ui.auth

import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.restorecredentials.RestoreCredentials
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthFlowCompletionTest {

  private val dispatcher = StandardTestDispatcher()
  private val creationJob = SupervisorJob()
  private lateinit var defaultCreationScope: CoroutineScope

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.markAuthFlowComplete() } just runs
    mockkObject(RestoreCredentials)
    coEvery { RestoreCredentials.createIfMissing() } returns ClerkResult.success(Unit)
    defaultCreationScope = RestoreCredentialCreation.scope
    RestoreCredentialCreation.scope = CoroutineScope(creationJob + dispatcher)
  }

  @After
  fun tearDown() {
    RestoreCredentialCreation.scope = defaultCreationScope
    unmockkAll()
  }

  @Test
  fun `completing auth with the flag off makes no restore credential calls`() {
    var completions = 0

    completeAuthFlow(
      isDismissible = true,
      enableRestoreCredentials = false,
      onAuthComplete = { completions++ },
    )
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, completions)
    coVerify(exactly = 0) { RestoreCredentials.createIfMissing() }
    coVerify(exactly = 0) { RestoreCredentials.create(any()) }
  }

  @Test
  fun `completing auth with the flag on creates a restore credential after completing`() {
    var completions = 0

    completeAuthFlow(
      isDismissible = false,
      enableRestoreCredentials = true,
      onAuthComplete = { completions++ },
    )

    assertEquals(1, completions)
    coVerify(exactly = 0) { RestoreCredentials.createIfMissing() }
    dispatcher.scheduler.advanceUntilIdle()
    coVerify(exactly = 1) { RestoreCredentials.createIfMissing() }
  }

  @Test
  fun `a failed restore credential creation does not reach the user`() {
    coEvery { RestoreCredentials.createIfMissing() } coAnswers
      {
        delay(1_000)
        ClerkResult.unknownFailure(IllegalStateException("Credential Manager unavailable"))
      }
    var completions = 0

    completeAuthFlow(
      isDismissible = true,
      enableRestoreCredentials = true,
      onAuthComplete = { completions++ },
    )
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, completions)
    coVerify(exactly = 1) { RestoreCredentials.createIfMissing() }
    assertTrue(creationJob.isActive)
  }

  @Test
  fun `a restore credential creation that throws does not reach the user`() {
    coEvery { RestoreCredentials.createIfMissing() } coAnswers
      {
        delay(1_000)
        throw IllegalStateException("Credential Manager crashed")
      }
    var completions = 0

    completeAuthFlow(
      isDismissible = true,
      enableRestoreCredentials = true,
      onAuthComplete = { completions++ },
    )
    dispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, completions)
    coVerify(exactly = 1) { RestoreCredentials.createIfMissing() }
    assertTrue(creationJob.isActive)
  }
}
