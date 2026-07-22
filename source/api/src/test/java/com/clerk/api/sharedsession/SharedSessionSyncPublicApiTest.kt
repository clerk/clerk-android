package com.clerk.api.sharedsession

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.SharedSessionSyncConfig
import com.clerk.api.network.model.client.Client
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SharedSessionSyncPublicApiTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    Clerk.reset()
    StorageHelper.reset(context)
  }

  @After
  fun tearDown() {
    Clerk.reset()
    StorageHelper.reset(context)
  }

  @Test
  fun `reloadFromSharedStorage applies persisted matching snapshot`() = runTest {
    Clerk.updateClient(Client(id = "local", updatedAt = 1_000), serverFetchAtMillis = 100)
    val sharedClient = Client(id = "shared", updatedAt = 2_000)
    val snapshot =
      SharedSessionSyncSnapshot(
        instanceId = SharedSessionSyncSnapshot.instanceId(PUBLISHABLE_KEY),
        auth =
          SharedSessionSyncSnapshot.AuthSnapshot(
            state = SharedSessionSyncSnapshot.State.SET,
            version = "shared-version",
            serverFetchAtMillis = 200,
            client = sharedClient,
          ),
      )
    StorageHelper.saveValue(
      StorageKey.SHARED_SESSION_SYNC_SNAPSHOT,
      SharedSessionSyncSnapshot.encode(snapshot),
    )
    Clerk.configureSharedSessionSync(
      context = context,
      publishableKey = PUBLISHABLE_KEY,
      config = SharedSessionSyncConfig.enabled,
    )

    val changed = Clerk.reloadFromSharedStorage()

    assertTrue(changed)
    assertEquals(sharedClient, Clerk.client)
    assertEquals(200L, Clerk.lastClientServerFetchAtMillis)
  }

  @Test
  fun `enabled config exposes a companion getter to published aar consumers`() {
    val getter = SharedSessionSyncConfig.Companion::class.java.getMethod("getEnabled")

    assertSame(SharedSessionSyncConfig.enabled, getter.invoke(SharedSessionSyncConfig.Companion))
  }

  private companion object {
    const val PUBLISHABLE_KEY = "pk_test_shared_session_sync"
  }
}
