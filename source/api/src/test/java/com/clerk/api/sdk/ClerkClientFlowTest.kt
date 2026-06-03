package com.clerk.api.sdk

import com.clerk.api.Clerk
import com.clerk.api.network.model.client.Client
import org.junit.Assert.assertEquals
import org.junit.Test

class ClerkClientFlowTest {

  @Test
  fun `updateClient updates clientFlow with latest client`() {
    val client = Client(id = "client_123")

    Clerk.updateClient(client)

    assertEquals(client, Clerk.clientFlow.value)
  }
}
