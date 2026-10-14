package com.clerk.api.passkeys

import com.clerk.api.Clerk
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyHelperTest {

  @Before
  fun setup() {
    mockkObject(Clerk)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `getDomain returns domain without www prefix`() {
    every { Clerk.baseUrl } returns "https://www.example.com/path"

    val result = PasskeyHelper.getDomain()

    assertEquals("example.com", result)
  }

  @Test
  fun `getDomain returns domain without protocol and path`() {
    every { Clerk.baseUrl } returns "https://clerk.example.com/api/v1"

    val result = PasskeyHelper.getDomain()

    assertEquals("clerk.example.com", result)
  }

  @Test
  fun `getDomain only strips a leading www`() {
    every { Clerk.baseUrl } returns "https://clerk.awww.dev"

    val result = PasskeyHelper.getDomain()

    assertEquals("clerk.awww.dev", result)
  }

  @Test
  fun `getDomain returns null when host is empty`() {
    every { Clerk.baseUrl } returns "https:///path"

    val result = PasskeyHelper.getDomain()

    assertNull(result)
  }

  @Test
  fun `getDomain returns null for malformed URL`() {
    every { Clerk.baseUrl } returns "not-a-url"

    val result = PasskeyHelper.getDomain()

    assertNull(result)
  }

  @Test
  fun `GetPasskeyRequest serializes to WebAuthn request option JSON`() {
    val request =
      GetPasskeyRequest(
        challenge = "test-challenge",
        allowCredentials =
          listOf(
            mapOf("type" to "public-key", "id" to "credential-1"),
            mapOf("type" to "public-key", "id" to "credential-2"),
          ),
        timeout = 60000L,
        userVerification = "required",
        rpId = "example.com",
      )

    val json = Json.parseToJsonElement(Json.encodeToString(request)).jsonObject

    assertEquals(
      setOf("challenge", "allowCredentials", "timeout", "userVerification", "rpId"),
      json.keys,
    )
    assertEquals("test-challenge", json.getValue("challenge").jsonPrimitive.content)
    assertEquals(60000L, json.getValue("timeout").jsonPrimitive.long)
    assertEquals("required", json.getValue("userVerification").jsonPrimitive.content)
    assertEquals("example.com", json.getValue("rpId").jsonPrimitive.content)
    assertEquals(
      listOf("credential-1", "credential-2"),
      json.getValue("allowCredentials").jsonArray.map {
        it.jsonObject.getValue("id").jsonPrimitive.content
      },
    )
  }

  @Test
  fun `constants have expected values`() {
    assertEquals("strategy", com.clerk.api.Constants.Fields.STRATEGY)
    assertEquals("passkey", com.clerk.api.auth.types.Strategy.Passkey.value)
  }
}
