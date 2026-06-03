package com.clerk.api.sdk

import com.clerk.api.Clerk
import com.clerk.api.client.ClientChanged
import com.clerk.api.client.ClientEvent
import com.clerk.api.network.model.client.Client
import com.clerk.api.session.Session
import com.clerk.api.user.User
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ClerkClientFlowTest {

  @Before
  fun setUp() {
    Clerk.updateClient(Client(id = "baseline_client"))
  }

  @Test
  fun `updateClient updates clientFlow with latest client`() {
    val client = Client(id = "client_123")

    Clerk.updateClient(client)

    assertEquals(client, Clerk.clientFlow.value)
  }

  @Test
  fun `updateClient emits ClientChanged client event when client changes`() = runTest {
    val client = Client(id = "client_123")
    val events = mutableListOf<ClientEvent>()
    val eventJob =
      launch(start = CoroutineStart.UNDISPATCHED) { Clerk.clientEvents.take(1).toList(events) }

    Clerk.updateClient(client)

    withTimeout(1_000) { eventJob.join() }

    assertTrue(events.single() is ClientChanged)
    assertEquals(client, (events.single() as ClientChanged).client)
  }

  @Test
  fun `updateClient does not emit ClientChanged client event when client is unchanged`() = runTest {
    val client = Client(id = "client_123")
    Clerk.updateClient(client)

    val events = mutableListOf<ClientEvent>()
    val eventJob =
      launch(start = CoroutineStart.UNDISPATCHED) { Clerk.clientEvents.take(1).toList(events) }

    Clerk.updateClient(client)

    withTimeoutOrNull(100) { eventJob.join() }
    eventJob.cancel()

    assertTrue(events.isEmpty())
  }

  @Test
  fun `updateClient emits ClientChanged client event when nested user property changes`() =
    runTest {
      val initialClient = clientWithUser(firstName = "Jane")
      val updatedClient = clientWithUser(firstName = "Janet")
      Clerk.updateClient(initialClient)

      val events = mutableListOf<ClientEvent>()
      val eventJob =
        launch(start = CoroutineStart.UNDISPATCHED) { Clerk.clientEvents.take(1).toList(events) }

      Clerk.updateClient(updatedClient)

      withTimeout(1_000) { eventJob.join() }

      val clientChanged = events.single() as ClientChanged
      assertEquals(updatedClient, clientChanged.client)
    }

  private fun clientWithUser(firstName: String): Client {
    val user =
      User(
        id = "user_123",
        imageUrl = "",
        hasImage = false,
        passkeys = emptyList(),
        passwordEnabled = false,
        phoneNumbers = emptyList(),
        totpEnabled = false,
        twoFactorEnabled = false,
        updatedAt = 0L,
        firstName = firstName,
      )
    val session =
      Session(
        id = "session_123",
        status = Session.SessionStatus.ACTIVE,
        expireAt = 10_000,
        lastActiveAt = 1_000,
        user = user,
        createdAt = 1_000,
        updatedAt = 1_000,
      )

    return Client(id = "client_123", sessions = listOf(session), lastActiveSessionId = session.id)
  }
}
