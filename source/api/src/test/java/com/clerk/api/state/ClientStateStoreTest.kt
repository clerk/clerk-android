package com.clerk.api.state

import com.clerk.api.configuration.CachedClerkState
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.session.Session
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ClientStateStoreTest {
  private val scheduler = TestCoroutineScheduler()
  private val storage = FakeCachedStateStorage()
  private var now = LOCAL_NOW_MILLIS

  private fun store(configured: Boolean = true) =
    ClientStateStore(
      clock = { now },
      cacheStorage = storage,
      cacheConfiguration = {
        if (configured) CachedStateConfiguration(publishableKey = "pk_test", baseUrl = BASE_URL)
        else null
      },
      persistenceDispatcher = StandardTestDispatcher(scheduler),
    )

  // region Ordering

  @Test
  fun `response fetched before applied state is dropped`() {
    val store = store()
    val newer = Client(id = "client_newer")
    val older = Client(id = "client_older")

    assertTrue(store.applyClientResponse(newer, serverDateMillis = 2_000))
    assertFalse(store.applyClientResponse(older, serverDateMillis = 1_000))

    assertEquals(newer, store.client)
    assertEquals(newer, store.clientFlow.value)
    assertEquals(2_000L, store.serverFetchAtMillis)
  }

  @Test
  fun `responses from the same server second apply in arrival order`() {
    val store = store()

    store.applyClientResponse(Client(id = "client_first"), serverDateMillis = 1_000)
    store.applyClientResponse(Client(id = "client_second"), serverDateMillis = 1_000)

    assertEquals("client_second", store.client?.id)
  }

  @Test
  fun `caller re-applying an older recorded response body is dropped`() {
    val store = store()
    val refreshed = Client(id = "client_123", signIn = null)
    val signedIn = clientWithSession("sess_new")
    // GET /client: the middleware records the body; the caller applies it after awaiting the call.
    store.recordClientResponse(refreshed, serverDateMillis = 1_000)
    // A sign-in completes while the refresh result is still in flight to its caller.
    store.applyClientResponse(signedIn, serverDateMillis = 2_000)

    assertFalse(store.applyClient(refreshed))

    assertEquals(signedIn, store.client)
    assertEquals("sess_new", store.sessionFlow.value?.id)
  }

  @Test
  fun `caller re-applying the newest recorded response body uses its server date`() {
    val store = store()
    val refreshed = Client(id = "client_123")
    store.applyClientResponse(Client(id = "client_123", updatedAt = 1), serverDateMillis = 1_000)
    store.recordClientResponse(refreshed, serverDateMillis = 2_000)

    assertTrue(store.applyClient(refreshed))

    assertEquals(refreshed, store.client)
    assertEquals(2_000L, store.serverFetchAtMillis)
    assertFalse(store.applyClientResponse(Client(id = "client_old"), serverDateMillis = 1_500))
  }

  @Test
  fun `local mutations always apply and never advance the server watermark`() {
    val store = store()
    store.applyClientResponse(Client(id = "client_server"), serverDateMillis = 1_000)
    // A device clock far ahead of the server must not make later responses look stale.
    now = 9_999_999

    assertTrue(store.applyClient(Client(id = "client_local")))
    assertEquals(9_999_999L, store.serverFetchAtMillis)

    assertTrue(store.applyClientResponse(Client(id = "client_fresh"), serverDateMillis = 1_001))
    assertEquals("client_fresh", store.client?.id)
  }

  @Test
  fun `response without a server date applies unordered`() {
    val store = store()
    store.applyClientResponse(Client(id = "client_dated"), serverDateMillis = 2_000)

    assertTrue(store.applyClientResponse(Client(id = "client_undated"), serverDateMillis = null))

    assertEquals("client_undated", store.client?.id)
    assertEquals(LOCAL_NOW_MILLIS, store.serverFetchAtMillis)
  }

  @Test
  fun `authoritative replacement ignores ordering`() {
    val store = store()
    store.applyClientResponse(Client(id = "client_dated"), serverDateMillis = 2_000)

    store.replaceClient(Client(id = "client_shared"), serverFetchAtMillis = 100)

    assertEquals("client_shared", store.client?.id)
    assertEquals(100L, store.serverFetchAtMillis)
  }

  @Test
  fun `concurrent writers settle on the newest response with consistent flows`() {
    val store = store()
    val committedRevisions = Collections.synchronizedList(mutableListOf<Long>())
    store.listener =
      object : ClientStateStore.Listener {
        override fun onClientCommitted(commit: ClientStateStore.ClientCommit) {
          committedRevisions += commit.revision
        }
      }
    val threads = 16
    val writesPerThread = 200
    val executor = Executors.newFixedThreadPool(threads)
    val start = CountDownLatch(1)
    val done = CountDownLatch(threads)

    repeat(threads) { thread ->
      executor.execute {
        start.await()
        repeat(writesPerThread) { write ->
          // Unique, interleaved server dates so arrival order differs from fetch order.
          val date = (write * threads + thread).toLong()
          store.applyClientResponse(clientWithSession("sess_$date", clientId = "c_$date"), date)
          // Interleave local read-modify-write mutations; they must not resurrect older clients.
          if (thread % 2 == 0) store.mutateClient { it.copy(signIn = null) }
        }
        done.countDown()
      }
    }
    start.countDown()
    assertTrue(done.await(30, TimeUnit.SECONDS))
    executor.shutdown()

    val newestDate = (writesPerThread * threads - 1).toLong()
    assertEquals("c_$newestDate", store.client?.id)
    assertEquals(store.client, store.clientFlow.value)
    assertEquals("sess_$newestDate", store.sessionFlow.value?.id)
    assertEquals(store.sessionFlow.value?.user, store.userFlow.value)
    assertEquals(listOf(store.sessionFlow.value), store.sessionsFlow.value)
    assertEquals(committedRevisions.size, committedRevisions.toSet().size)
  }

  // endregion

  // region Persistence

  @Test
  fun `persistence runs off the calling thread and coalesces bursts into one write`() {
    val store = store()
    store.applyEnvironment(testEnvironment())

    repeat(50) { index ->
      store.applyClientResponse(Client(id = "client_$index"), serverDateMillis = index.toLong())
    }

    assertTrue("no write on the calling thread", storage.saved.isEmpty())
    scheduler.advanceUntilIdle()

    assertEquals(1, storage.saved.size)
    assertEquals("client_49", storage.saved.single().client.id)
    assertEquals(49L, storage.saved.single().clientServerFetchAtMillis)
  }

  @Test
  fun `incomplete state is not persisted`() {
    val store = store()

    store.applyClientResponse(Client(id = "client_only"), serverDateMillis = 1)
    scheduler.advanceUntilIdle()

    assertTrue(storage.saved.isEmpty())
  }

  @Test
  fun `sign-out is persisted before the write returns`() {
    val store = store()
    store.applyEnvironment(testEnvironment())
    store.applyClientResponse(clientWithSession("sess_123"), serverDateMillis = 1)
    scheduler.advanceUntilIdle()
    storage.saved.clear()

    store.applyClientResponse(Client(id = "client_123"), serverDateMillis = 2)

    assertEquals(1, storage.saved.size)
    assertTrue(storage.saved.single().client.sessions.isEmpty())
  }

  @Test
  fun `reset clears memory, ordering, and the persisted cache and cancels pending writes`() {
    val store = store()
    store.applyEnvironment(testEnvironment())
    store.applyClientResponse(clientWithSession("sess_123"), serverDateMillis = 5_000)

    store.reset()
    scheduler.advanceUntilIdle()

    assertTrue("pending write must not land after reset", storage.saved.isEmpty())
    assertEquals(1, storage.clearCount)
    assertNull(store.clientFlow.value)
    assertEquals(Client(), store.client)
    assertNull(store.sessionFlow.value)
    assertNull(store.userFlow.value)
    assertNull(store.environment)
    assertNull(store.serverFetchAtMillis)
    assertTrue(store.applyClientResponse(Client(id = "client_after"), serverDateMillis = 1))
  }

  // endregion

  @Test
  fun `session listener sees transitions in commit order`() {
    val store = store(configured = false)
    val transitions = mutableListOf<Pair<String?, String?>>()
    store.listener =
      object : ClientStateStore.Listener {
        override fun onSessionStateCommitted(previous: Session?, current: Session?) {
          transitions += previous?.id to current?.id
        }
      }
    val signedIn = clientWithSession("sess_123")

    store.applyClient(signedIn)
    store.clearSessionState()
    store.recomputeSessionState()

    assertEquals(
      listOf(null to "sess_123", "sess_123" to null, null to "sess_123"),
      transitions,
    )
    assertSame(signedIn, store.client)
  }

  private fun clientWithSession(sessionId: String, clientId: String = "client_123"): Client =
    Client(
      id = clientId,
      sessions =
        listOf(
          Session(
            id = sessionId,
            status = Session.SessionStatus.ACTIVE,
            expireAt = 10_000,
            lastActiveAt = 1_000,
            createdAt = 1_000,
            updatedAt = 1_000,
          )
        ),
      lastActiveSessionId = sessionId,
    )

  private fun testEnvironment(): Environment =
    Environment(
      authConfig = AuthConfig(singleSessionMode = false),
      displayConfig =
        DisplayConfig(
          applicationName = "Test App",
          branded = true,
          logoImageUrl = "https://example.com/logo.png",
          homeUrl = "/",
          privacyPolicyUrl = null,
          termsUrl = null,
          googleOneTapClientId = null,
        ),
      userSettings =
        UserSettings(
          attributes = emptyMap(),
          signUp =
            UserSettings.SignUpUserSettings(
              customActionRequired = false,
              progressive = false,
              mode = "public",
              legalConsentEnabled = false,
            ),
          social = emptyMap(),
          actions = UserSettings.Actions(),
          passkeySettings = null,
        ),
    )

  private class FakeCachedStateStorage : CachedStateStorage {
    val saved = Collections.synchronizedList(mutableListOf<CachedClerkState>())
    var clearCount = 0

    override fun save(state: CachedClerkState) {
      saved += state
    }

    override fun clear() {
      saved.clear()
      clearCount += 1
    }
  }

  private companion object {
    const val LOCAL_NOW_MILLIS = 42L
    const val BASE_URL = "https://example.clerk.accounts.dev"
  }
}
