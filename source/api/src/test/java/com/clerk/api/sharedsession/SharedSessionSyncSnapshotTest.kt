package com.clerk.api.sharedsession

import com.clerk.api.network.model.client.Client
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedSessionSyncSnapshotTest {
  @Test
  fun `newer server fetch applies`() {
    val current = auth(client = Client(id = "local", updatedAt = 3_000), fetchAt = 100)
    val incoming = auth(client = Client(id = "shared", updatedAt = 2_000), fetchAt = 200)

    assertEquals(
      SharedSessionSyncClientDecision.APPLY,
      decideSharedClientSnapshot(incoming, current),
    )
  }

  @Test
  fun `older server fetch is rejected even with newer client updated at`() {
    val current = auth(client = Client(id = "local", updatedAt = 2_000), fetchAt = 200)
    val incoming = auth(client = Client(id = "shared", updatedAt = 3_000), fetchAt = 100)

    assertEquals(
      SharedSessionSyncClientDecision.REJECT_STALE,
      decideSharedClientSnapshot(incoming, current),
    )
  }

  @Test
  fun `equal fetch uses client updated at as tie breaker`() {
    val current = auth(client = Client(id = "local", updatedAt = 2_000), fetchAt = 200)
    val incoming = auth(client = Client(id = "shared", updatedAt = 3_000), fetchAt = 200)

    assertEquals(
      SharedSessionSyncClientDecision.APPLY,
      decideSharedClientSnapshot(incoming, current),
    )
  }

  @Test
  fun `equal fetch does not resurrect client over clear`() {
    val current = cleared(fetchAt = 200)
    val incoming = auth(client = Client(id = "shared", updatedAt = 3_000), fetchAt = 200)

    assertEquals(
      SharedSessionSyncClientDecision.REJECT_STALE,
      decideSharedClientSnapshot(incoming, current),
    )
  }

  @Test
  fun `newer clear applies`() {
    val current = auth(client = Client(id = "local", updatedAt = 2_000), fetchAt = 100)

    assertEquals(
      SharedSessionSyncClientDecision.APPLY,
      decideSharedClientSnapshot(cleared(fetchAt = 200), current),
    )
  }

  @Test
  fun `matching snapshot is ignored`() {
    val client = Client(id = "client", updatedAt = 2_000)
    val current = auth(client = client, fetchAt = 200)
    val incoming = current.copy(version = "incoming")

    assertEquals(
      SharedSessionSyncClientDecision.IGNORE,
      decideSharedClientSnapshot(incoming, current),
    )
  }

  @Test
  fun `snapshot codec round trips typed state`() {
    val snapshot =
      SharedSessionSyncSnapshot(
        instanceId = "instance",
        auth = auth(client = Client(id = "client", updatedAt = 2_000), fetchAt = 200),
        deviceToken =
          SharedSessionSyncSnapshot.DeviceTokenSnapshot(
            state = SharedSessionSyncSnapshot.State.SET,
            version = "device-version",
            changedAtMillis = 300,
            value = "device-token",
          ),
      )

    assertEquals(
      snapshot,
      SharedSessionSyncSnapshot.decode(SharedSessionSyncSnapshot.encode(snapshot)),
    )
  }

  private fun auth(client: Client, fetchAt: Long) =
    SharedSessionSyncSnapshot.AuthSnapshot(
      state = SharedSessionSyncSnapshot.State.SET,
      version = "version-$fetchAt",
      serverFetchAtMillis = fetchAt,
      client = client,
    )

  private fun cleared(fetchAt: Long) =
    SharedSessionSyncSnapshot.AuthSnapshot(
      state = SharedSessionSyncSnapshot.State.CLEARED,
      version = "version-$fetchAt",
      serverFetchAtMillis = fetchAt,
    )
}
