package com.clerk.api.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObfuscatedAccountIdTest {

  @Test
  fun `derivation is the SHA-256 hex digest of the user id`() {
    // Well-known SHA-256 test vector: sha256("abc").
    assertEquals(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
      ObfuscatedAccountId.fromUserId("abc"),
    )
  }

  @Test
  fun `derivation is deterministic for the same user id`() {
    val userId = "user_2yGkLmN0pQrStUvWxYz"
    assertEquals(ObfuscatedAccountId.fromUserId(userId), ObfuscatedAccountId.fromUserId(userId))
  }

  @Test
  fun `derived id respects Google's 64 character limit`() {
    val obfuscated = ObfuscatedAccountId.fromUserId("user_2yGkLmN0pQrStUvWxYz")
    assertEquals(64, obfuscated.length)
    assertTrue(obfuscated.all { it.isDigit() || it in 'a'..'f' })
  }

  @Test
  fun `different user ids derive different identifiers`() {
    assertTrue(ObfuscatedAccountId.fromUserId("user_1") != ObfuscatedAccountId.fromUserId("user_2"))
  }
}
