package com.clerk.api.user

import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.toFormFields
import com.clerk.api.phonenumber.PhoneNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserTest {

  @Test
  fun `UpdateParams should exclude unsafeMetadata when null`() {
    val params = User.UpdateParams(firstName = "Jane", lastName = "Smith", unsafeMetadata = null)

    val paramsMap = params.toFormFields()

    assertNotNull("Map should not be null", paramsMap)
    assertEquals("Should include firstName", "Jane", paramsMap["first_name"])
    assertEquals("Should include lastName", "Smith", paramsMap["last_name"])
    assertTrue(
      "Should not include unsafe_metadata when null",
      !paramsMap.containsKey("unsafe_metadata"),
    )
  }

  @Test
  fun `UpdateParams form fields are the profile, public metadata and private metadata fields`() {
    val publicMetadataJson = """{"role":"admin"}"""
    val privateMetadataJson = """{"internal_id":"12345"}"""

    val paramsMap =
      User.UpdateParams(
          firstName = "Alice",
          publicMetadata = publicMetadataJson,
          privateMetadata = privateMetadataJson,
        )
        .toFormFields()

    assertEquals(
      mapOf(
        "first_name" to "Alice",
        "public_metadata" to publicMetadataJson,
        "private_metadata" to privateMetadataJson,
      ),
      paramsMap,
    )
  }

  @Test
  fun `UpdateMetadataParams should exclude unsafe_metadata when null`() {
    val params = User.UpdateMetadataParams(unsafeMetadata = null)

    val paramsMap = params.toFormFields()

    assertTrue("Map should be empty when no fields are provided", paramsMap.isEmpty())
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
