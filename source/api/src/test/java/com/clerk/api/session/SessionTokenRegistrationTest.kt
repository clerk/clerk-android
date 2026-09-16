package com.clerk.api.session

import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionTokenRegistrationTest {
  private val session =
    Session(
      id = "session_123",
      status = Session.SessionStatus.ACTIVE,
      expireAt = 4_000_000_000_000,
      lastActiveAt = 0,
      createdAt = 0,
      updatedAt = 0,
    )
  private val api = mockk<SessionApi>()
  private val freshToken = TokenResource("fresh.token.value")

  @Before
  fun setup() {
    SessionTokensCache.clear()
    mockkObject(Clerk, ClerkApi, ClerkLog)
    every { Clerk.clientFlow } returns MutableStateFlow(Client(sessions = listOf(session)))
    every { Clerk.environment } returns null
    every { ClerkApi.session } returns api
    coEvery { api.tokens(session.id, "", null, null) } returns ClerkResult.success(freshToken)
  }

  @After
  fun tearDown() {
    unmockkAll()
    SessionTokensCache.clear()
  }

  @Test
  fun `request after reverification cannot join a late stale registration`() {
    verifyLateRegistration { it.invalidateSession(session.id) }
  }

  @Test
  fun `request after reset cannot join a late stale registration`() {
    verifyLateRegistration { it.reset() }
  }

  @Test
  fun `session lookup and runtime generation cannot straddle a reset`() = runBlocking {
    val fetcher = SessionTokenFetcher()
    val oldSession = session.copy(lastActiveOrganizationId = "org_old")
    val currentSession = session.copy(lastActiveOrganizationId = "org_current")
    val client = MutableStateFlow(Client(sessions = listOf(oldSession)))
    every { Clerk.clientFlow } returns client
    coEvery { api.tokens(session.id, "org_old", null, null) } returns
      ClerkResult.success(TokenResource("stale.token.value"))
    coEvery { api.tokens(session.id, "org_current", null, null) } returns
      ClerkResult.success(freshToken)
    val runtimeLock =
      SessionTokenFetcher::class.java.getDeclaredField("runtimeLock").let {
        it.isAccessible = true
        it.get(fetcher)
      }
    val callerThread = AtomicReference<Thread>()
    val callerStarted = CountDownLatch(1)

    Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
      val request =
        synchronized(runtimeLock) {
          val request =
            async(dispatcher) {
              callerThread.set(Thread.currentThread())
              callerStarted.countDown()
              fetcher.getToken(session)
            }
          assertTrue(callerStarted.await(5, TimeUnit.SECONDS))
          awaitBlockedOn(callerThread.get(), runtimeLock)

          // Reset while context creation is blocked, replacing the session's organization.
          client.value = Client(sessions = listOf(currentSession))
          fetcher.reset()
          request
        }

      assertEquals(freshToken, withTimeout(5_000) { request.await() })
      assertEquals(freshToken, SessionTokensCache.getToken(currentSession.tokenCacheKey(null)))
      assertNull(SessionTokensCache.getToken(oldSession.tokenCacheKey(null)))
      coVerify(exactly = 0) { api.tokens(session.id, "org_old", null, null) }
      coVerify(exactly = 1) { api.tokens(session.id, "org_current", null, null) }
    }
  }

  private fun awaitBlockedOn(thread: Thread, lock: Any) {
    val threads = ManagementFactory.getThreadMXBean()
    val lockId = System.identityHashCode(lock)
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
    while (
      threads.getThreadInfo(thread.id)?.lockInfo?.identityHashCode != lockId &&
        System.nanoTime() < deadline
    ) {
      Thread.yield()
    }
    assertEquals(lockId, threads.getThreadInfo(thread.id)?.lockInfo?.identityHashCode)
  }

  private fun verifyLateRegistration(invalidate: (SessionTokenFetcher) -> Unit) = runBlocking {
    val fetcher = SessionTokenFetcher()
    val tasks = RegistrationBarrierMap()
    val beforeRegistration = CountDownLatch(1)
    val allowRegistration = CountDownLatch(1)
    SessionTokenFetcher::class.java.getDeclaredField("tokenTasks").apply {
      isAccessible = true
      set(fetcher, tasks)
    }
    every { ClerkLog.d(any()) } answers
      {
        if (
          Thread.currentThread() == tasks.oldCaller.get() &&
            firstArg<String>().startsWith("Fetching token for session")
        ) {
          beforeRegistration.countDown()
          check(allowRegistration.await(5, TimeUnit.SECONDS))
        }
        0
      }
    Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
      withTimeout(10_000) {
        val oldRequest =
          async(dispatcher) {
            tasks.oldCaller.set(Thread.currentThread())
            try {
              fetcher.getToken(session)
            } finally {
              tasks.oldRegisteredOrFinished.complete(Unit)
            }
          }
        try {
          assertTrue(beforeRegistration.await(5, TimeUnit.SECONDS))
          invalidate(fetcher)
          allowRegistration.countDown()
          tasks.oldRegisteredOrFinished.await()
          val newRequest = async(Dispatchers.Default) { fetcher.getToken(session) }
          tasks.newCallerRegistered.await()
          tasks.allowOldCaller.countDown()

          assertNull(oldRequest.await())
          assertEquals(freshToken, newRequest.await())
          coVerify(exactly = 1) { api.tokens(session.id, "", null, null) }
        } finally {
          allowRegistration.countDown()
          tasks.allowOldCaller.countDown()
        }
      }
    }
  }

  // Hold a stale registration long enough for the new caller to join it on the broken path.
  private class RegistrationBarrierMap :
    ConcurrentHashMap<String, CompletableDeferred<TokenResource?>>() {
    val oldCaller = AtomicReference<Thread>()
    val oldRegisteredOrFinished = CompletableDeferred<Unit>()
    val newCallerRegistered = CompletableDeferred<Unit>()
    val allowOldCaller = CountDownLatch(1)

    override fun putIfAbsent(
      key: String,
      value: CompletableDeferred<TokenResource?>,
    ): CompletableDeferred<TokenResource?>? {
      val existing = super.putIfAbsent(key, value)
      if (Thread.currentThread() == oldCaller.get()) {
        oldRegisteredOrFinished.complete(Unit)
        check(allowOldCaller.await(5, TimeUnit.SECONDS))
      } else {
        newCallerRegistered.complete(Unit)
      }
      return existing
    }
  }
}
