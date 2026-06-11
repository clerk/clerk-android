package com.clerk.api.sdk

import com.clerk.api.Clerk
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
  fun `refreshClient fetches and updates clientFlow`() = runTest {
    mockkObject(Client.Companion)
    try {
      val client = Client(id = "client_123")
      coEvery { Client.get() } returns ClerkResult.success(client)

      val result = Clerk.refreshClient()

      assertEquals(client, Clerk.clientFlow.value)
      assertEquals(client, (result as ClerkResult.Success).value)
    } finally {
      unmockkObject(Client.Companion)
    }
  }
}
