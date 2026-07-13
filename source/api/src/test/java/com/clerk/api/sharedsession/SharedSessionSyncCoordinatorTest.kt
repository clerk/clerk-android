package com.clerk.api.sharedsession

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.network.model.client.Client
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SharedSessionSyncCoordinatorTest {
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
  fun `public reload behavior applies newer sibling client without echoing notification`() =
    runTest {
      val local = Client(id = "local", updatedAt = 1_000)
      val shared = Client(id = "shared", updatedAt = 2_000)
      Clerk.updateClient(local, serverFetchAtMillis = 100)
      val transport =
        FakeTransport(
          peers = listOf(snapshot(auth = auth(shared, fetchAt = 200, version = "shared")))
        )
      val coordinator = coordinator(transport)
      coordinator.start()

      val changed = coordinator.reloadFromSharedStorage()

      assertTrue(changed)
      assertEquals("shared", Clerk.client.id)
      assertEquals(200L, Clerk.lastClientServerFetchAtMillis)
      assertEquals("shared", transport.local?.auth?.version)
      assertEquals(0, transport.notificationCount)
      coordinator.close()
    }

  @Test
  fun `newer sibling clear removes local auth state`() = runTest {
    Clerk.updateClient(Client(id = "local", updatedAt = 1_000), serverFetchAtMillis = 100)
    val transport =
      FakeTransport(peers = listOf(snapshot(auth = cleared(fetchAt = 200, version = "clear"))))
    val coordinator = coordinator(transport)
    coordinator.start()

    val changed = coordinator.reloadFromSharedStorage()

    assertTrue(changed)
    assertEquals(Client(), Clerk.client)
    assertNull(transport.local?.auth?.client)
    assertEquals(SharedSessionSyncSnapshot.State.CLEARED, transport.local?.auth?.state)
    coordinator.close()
  }

  @Test
  fun `stale sibling snapshot is rejected and repaired from local state`() = runTest {
    val local = Client(id = "local", updatedAt = 2_000)
    Clerk.updateClient(local, serverFetchAtMillis = 200)
    val transport =
      FakeTransport(
        peers =
          listOf(snapshot(auth = auth(Client(id = "shared", updatedAt = 3_000), 100, "stale")))
      )
    val coordinator = coordinator(transport)
    coordinator.start()

    val changed = coordinator.reloadFromSharedStorage()

    assertFalse(changed)
    assertEquals("local", Clerk.client.id)
    assertEquals("local", transport.local?.auth?.client?.id)
    assertTrue(transport.local?.auth?.version != "stale")
    assertEquals(1, transport.notificationCount)
    coordinator.close()
  }

  @Test
  fun `local client change publishes opaque revision`() {
    val transport = FakeTransport()
    val coordinator = coordinator(transport)
    coordinator.start()

    coordinator.handleClientChange(
      client = Client(id = "local", updatedAt = 2_000),
      serverFetchAtMillis = 200,
    )

    assertEquals("local", transport.local?.auth?.client?.id)
    assertTrue(runCatching { java.util.UUID.fromString(transport.local?.auth?.version) }.isSuccess)
    assertEquals(1, transport.notificationCount)
    coordinator.close()
  }

  @Test
  fun `sibling device token is copied into encrypted local storage`() = runTest {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "local-token")
    val transport =
      FakeTransport(
        peers =
          listOf(
            snapshot(
              deviceToken =
                SharedSessionSyncSnapshot.DeviceTokenSnapshot(
                  state = SharedSessionSyncSnapshot.State.SET,
                  version = "shared-device",
                  changedAtMillis = 200,
                  value = "shared-token",
                )
            )
          )
      )
    val coordinator = coordinator(transport)
    coordinator.start()

    val changed = coordinator.reloadFromSharedStorage()

    assertTrue(changed)
    assertEquals("shared-token", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertEquals("shared-device", transport.local?.deviceToken?.version)
    assertEquals(0, transport.notificationCount)
    coordinator.close()
  }

  @Test
  fun `local device token changes publish set and cleared revisions`() {
    val transport = FakeTransport()
    val coordinator = coordinator(transport)
    coordinator.start()

    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "local-token")

    val setVersion = transport.local?.deviceToken?.version
    assertEquals(SharedSessionSyncSnapshot.State.SET, transport.local?.deviceToken?.state)
    assertEquals("local-token", transport.local?.deviceToken?.value)
    assertEquals(1, transport.notificationCount)

    StorageHelper.deleteValue(StorageKey.DEVICE_TOKEN)

    assertEquals(SharedSessionSyncSnapshot.State.CLEARED, transport.local?.deviceToken?.state)
    assertNull(transport.local?.deviceToken?.value)
    assertTrue(setVersion != transport.local?.deviceToken?.version)
    assertEquals(2, transport.notificationCount)
    coordinator.close()
  }

  private fun coordinator(transport: FakeTransport): SharedSessionSyncCoordinator =
    SharedSessionSyncCoordinator(instanceId = INSTANCE_ID, transport = transport, clock = { 500 })

  private fun snapshot(
    auth: SharedSessionSyncSnapshot.AuthSnapshot? = null,
    deviceToken: SharedSessionSyncSnapshot.DeviceTokenSnapshot? = null,
  ) = SharedSessionSyncSnapshot(instanceId = INSTANCE_ID, auth = auth, deviceToken = deviceToken)

  private fun auth(client: Client, fetchAt: Long, version: String) =
    SharedSessionSyncSnapshot.AuthSnapshot(
      state = SharedSessionSyncSnapshot.State.SET,
      version = version,
      serverFetchAtMillis = fetchAt,
      client = client,
    )

  private fun cleared(fetchAt: Long, version: String) =
    SharedSessionSyncSnapshot.AuthSnapshot(
      state = SharedSessionSyncSnapshot.State.CLEARED,
      version = version,
      serverFetchAtMillis = fetchAt,
    )

  private class FakeTransport(
    var local: SharedSessionSyncSnapshot? = null,
    var peers: List<SharedSessionSyncSnapshot> = emptyList(),
  ) : SharedSessionSyncTransport {
    var notificationCount = 0
    private var handler: (() -> Unit)? = null

    override fun loadLocalSnapshot(): SharedSessionSyncSnapshot? = local

    override fun loadPeerSnapshots(): List<SharedSessionSyncSnapshot> = peers

    override fun saveLocalSnapshot(snapshot: SharedSessionSyncSnapshot, notifyPeers: Boolean) {
      local = snapshot
      if (notifyPeers) notificationCount += 1
    }

    override fun start(onPeerChange: () -> Unit) {
      handler = onPeerChange
    }

    override fun close() {
      handler = null
    }
  }

  private companion object {
    const val INSTANCE_ID = "instance"
  }
}
