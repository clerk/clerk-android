package com.clerk.sdk.model.signin.internal

import com.clerk.automap.annotation.AutoMap
import com.clerk.sdk.Clerk.signIn
import com.clerk.sdk.model.signin.SignIn.PrepareFirstFactorParams
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val EMAIL_CODE = "email_code"
private const val PHONE_CODE = "phone_code"

sealed interface FirstFactorFormData {
  val strategy: String

  @AutoMap
  @Serializable
  data class EmailCode(
    @SerialName("email_address_id") val emailAddressId: String,
    override val strategy: String = "email_code",
  ) : FirstFactorFormData

  @AutoMap
  @Serializable
  data class PhoneCode(
    @SerialName("phone_number_id") val phoneNumberId: String,
    override val strategy: String = "phone_code",
  ) : FirstFactorFormData

  @AutoMap
  @Serializable
  data class ResetPasswordEmailCode(
    @SerialName("email_address_id") val emailAddressId: String,
    override val strategy: String = "reset_password_email_code",
  ) : FirstFactorFormData

  @AutoMap
  @Serializable
  data class ResetPasswordPhoneCode(
    @SerialName("phone_number_id") val phoneNumberId: String,
    override val strategy: String = "reset_password_phone_code",
  ) : FirstFactorFormData

  data class Unknown(override val strategy: String = "Unknown") : FirstFactorFormData
}

fun PrepareFirstFactorParams.Strategy.toFormData(): FirstFactorFormData {
  val firstFactors =
    requireNotNull(signIn?.supportedFirstFactors) { "No supported first factors set" }
  val emailId = firstFactors.find { it.strategy == EMAIL_CODE }?.emailAddressId
  val phoneId = firstFactors.find { it.strategy == PHONE_CODE }?.phoneNumberId
  return when (this) {
    PrepareFirstFactorParams.Strategy.EMAIL_CODE -> FirstFactorFormData.EmailCode(emailId!!)

    PrepareFirstFactorParams.Strategy.PHONE_CODE -> FirstFactorFormData.PhoneCode(phoneId!!)
    PrepareFirstFactorParams.Strategy.RESET_PASSWORD_EMAIL_CODE ->
      FirstFactorFormData.ResetPasswordEmailCode(emailId!!)
    PrepareFirstFactorParams.Strategy.RESET_PASSWORD_PHONE_CODE ->
      FirstFactorFormData.ResetPasswordPhoneCode(phoneId!!)
    else -> FirstFactorFormData.Unknown()
  }
}
