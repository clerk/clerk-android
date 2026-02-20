package com.clerk.api.user

import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.phonenumber.PhoneNumber
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserTest {

  @Test
  fun `UpdateParams should include unsafeMetadata when provided`() {
    // Given
    val unsafeMetadataJson = """{"birthday":"11-30-1969"}"""
    val params =
      User.UpdateParams(firstName = "John", lastName = "Doe", unsafeMetadata = unsafeMetadataJson)

    // When
    val paramsMap = params.toMap()

    // Then
    assertNotNull("Map should not be null", paramsMap)
    assertEquals("Should include firstName", "John", paramsMap["first_name"])
    assertEquals("Should include lastName", "Doe", paramsMap["last_name"])
    assertEquals("Should include unsafe_metadata", unsafeMetadataJson, paramsMap["unsafe_metadata"])
  }

  @Test
  fun `UpdateParams should exclude unsafeMetadata when null`() {
    // Given
    val params = User.UpdateParams(firstName = "Jane", lastName = "Smith", unsafeMetadata = null)

    // When
    val paramsMap = params.toMap()

    // Then
    assertNotNull("Map should not be null", paramsMap)
    assertEquals("Should include firstName", "Jane", paramsMap["first_name"])
    assertEquals("Should include lastName", "Smith", paramsMap["last_name"])
    assertTrue(
      "Should not include unsafe_metadata when null",
      !paramsMap.containsKey("unsafe_metadata"),
    )
  }

  @Test
  fun `UpdateParams should include all metadata types when provided`() {
    // Given
    val publicMetadataJson = """{"role":"admin"}"""
    val privateMetadataJson = """{"internal_id":"12345"}"""
    val unsafeMetadataJson = """{"birthday":"11-30-1969"}"""

    val params =
      User.UpdateParams(
        firstName = "Alice",
        publicMetadata = publicMetadataJson,
        privateMetadata = privateMetadataJson,
        unsafeMetadata = unsafeMetadataJson,
      )

    // When
    val paramsMap = params.toMap()

    // Then
    assertNotNull("Map should not be null", paramsMap)
    assertEquals("Should include firstName", "Alice", paramsMap["first_name"])
    assertEquals("Should include public_metadata", publicMetadataJson, paramsMap["public_metadata"])
    assertEquals(
      "Should include private_metadata",
      privateMetadataJson,
      paramsMap["private_metadata"],
    )
    assertEquals("Should include unsafe_metadata", unsafeMetadataJson, paramsMap["unsafe_metadata"])
  }

  @Test
  fun `UpdateParams should only include unsafeMetadata when only it is provided`() {
    // Given
    val unsafeMetadataJson = """{"preferences":{"theme":"dark"}}"""

    val params = User.UpdateParams(unsafeMetadata = unsafeMetadataJson)

    // When
    val paramsMap = params.toMap()

    // Then
    assertNotNull("Map should not be null", paramsMap)
    assertEquals("Map should contain only one entry", 1, paramsMap.size)
    assertEquals("Should include unsafe_metadata", unsafeMetadataJson, paramsMap["unsafe_metadata"])
  }

  @Test
  fun `UpdateParams serialization should include unsafe_metadata field`() {
    // Given
    val unsafeMetadataJson = """{"custom":"data"}"""
    val params = User.UpdateParams(username = "testuser", unsafeMetadata = unsafeMetadataJson)

    // When
    val json = Json.encodeToString(params)

    // Then
    assertTrue("Serialized JSON should contain unsafe_metadata", json.contains("unsafe_metadata"))
    assertTrue(
      "Serialized JSON should contain the escaped metadata value",
      json.contains("""\"custom\":\"data\""""),
    )
  }

  @Test
  fun `UpdateParams with empty unsafeMetadata should be handled correctly`() {
    // Given
    val emptyMetadata = "{}"
    val params = User.UpdateParams(unsafeMetadata = emptyMetadata)

    // When
    val paramsMap = params.toMap()

    // Then
    assertNotNull("Map should not be null", paramsMap)
    assertEquals(
      "Should include unsafe_metadata with empty object",
      emptyMetadata,
      paramsMap["unsafe_metadata"],
    )
  }

  @Test
  fun `UpdateParams with complex nested unsafeMetadata should be preserved`() {
    // Given
    val complexMetadata =
      """{"user":{"preferences":{"theme":"dark","language":"en"},"settings":{"notifications":true}}}"""
    val params = User.UpdateParams(firstName = "Bob", unsafeMetadata = complexMetadata)

    // When
    val paramsMap = params.toMap()

    // Then
    assertNotNull("Map should not be null", paramsMap)
    assertEquals(
      "Should preserve complex nested metadata",
      complexMetadata,
      paramsMap["unsafe_metadata"],
    )
  }

  @Test
  fun `phoneNumbersAvailableForMfa excludes last first factor phone`() {
    val user = user(phoneNumbers = listOf(verifiedPhone(id = "phone_1")))

    val availableIds = user.phoneNumbersAvailableForMfa().map { it.id }

    assertTrue("Expected no eligible phones for MFA enrollment", availableIds.isEmpty())
  }

  @Test
  fun `phoneNumbersAvailableForMfa includes phone when verified email exists`() {
    val user =
      user(
        phoneNumbers = listOf(verifiedPhone(id = "phone_1")),
        emailAddresses = listOf(verifiedEmail(id = "email_1")),
      )

    val availableIds = user.phoneNumbersAvailableForMfa().map { it.id }

    assertEquals(listOf("phone_1"), availableIds)
  }

  @Test
  fun `phoneNumbersAvailableForMfa excludes phone when only alternative phone is reserved`() {
    val user =
      user(
        phoneNumbers =
          listOf(
            verifiedPhone(id = "phone_1"),
            verifiedPhone(id = "phone_2", reservedForSecondFactor = true),
          )
      )

    val availableIds = user.phoneNumbersAvailableForMfa().map { it.id }

    assertTrue("Expected no eligible phones for MFA enrollment", availableIds.isEmpty())
  }

  @Test
  fun `phoneNumbersAvailableForMfa includes verified phones when another remains available`() {
    val user =
      user(phoneNumbers = listOf(verifiedPhone(id = "phone_1"), verifiedPhone(id = "phone_2")))

    val availableIds = user.phoneNumbersAvailableForMfa().map { it.id }

    assertEquals(listOf("phone_1", "phone_2"), availableIds)
  }

  private fun user(
    phoneNumbers: List<PhoneNumber>,
    emailAddresses: List<EmailAddress> = emptyList(),
    username: String? = null,
  ): User {
    return User(
      id = "user_123",
      imageUrl = "",
      hasImage = false,
      passkeys = emptyList(),
      passwordEnabled = false,
      phoneNumbers = phoneNumbers,
      emailAddresses = emailAddresses,
      totpEnabled = false,
      twoFactorEnabled = false,
      updatedAt = 0L,
      username = username,
    )
  }

  private fun verifiedPhone(id: String, reservedForSecondFactor: Boolean = false): PhoneNumber {
    return PhoneNumber(
      id = id,
      phoneNumber = "+13012370655",
      verification = Verification(status = Verification.Status.VERIFIED),
      reservedForSecondFactor = reservedForSecondFactor,
    )
  }

  private fun verifiedEmail(id: String): EmailAddress {
    return EmailAddress(
      id = id,
      emailAddress = "sam@example.com",
      verification = Verification(status = Verification.Status.VERIFIED),
    )
  }
}
