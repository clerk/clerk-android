package com.clerk.api.sdk

import com.clerk.api.Clerk
import com.clerk.api.network.model.client.Client
import com.clerk.api.state.ClientStateStore
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ClerkClientOrNullTest {

  @After
  fun tearDown() {
    unmockkObject(Clerk)
  }

  @Test
  fun `a state store that has never received a client has no client`() {
    assertNull(ClientStateStore().client)
  }

  @Test
  fun `client throws and clientInitialized is false while clientOrNull is null`() {
    mockkObject(Clerk)
    every { Clerk.clientOrNull } returns null

    assertFalse(Clerk.clientInitialized)
    assertThrows(UninitializedPropertyAccessException::class.java) { Clerk.client }
  }

  @Test
  fun `clientOrNull returns the current client once Clerk has one`() {
    val client = Client(id = "client_123")

    Clerk.updateClient(client)

    assertEquals(client, Clerk.clientOrNull)
    assertEquals(client, Clerk.client)
  }
}
