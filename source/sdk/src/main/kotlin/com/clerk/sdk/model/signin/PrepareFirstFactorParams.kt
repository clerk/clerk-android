package com.clerk.sdk.model.signin

import com.clerk.automap.annotation.AutoMap
import com.clerk.sdk.network.requests.RequestParams
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is a private class meant to be used internally by the SDK. It is not meant to be used by
 * external developers. This class is used to prepare the first factor verification process. It gets
 * the serializable data class for the first factor verification process.
 */
@AutoMap
@Serializable
internal sealed interface PrepareFirstFactorParams {
  val strategy: String

  @AutoMap
  @Serializable
  data class EmailCode(
    @SerialName("email_address_id") val emailAddressId: String,
    override val strategy: String = "email_code",
  ) : PrepareFirstFactorParams

  @AutoMap
  @Serializable
  data class PhoneCode(
    @SerialName("phone_number_id") val phoneNumberId: String,
    override val strategy: String = "phone_code",
  ) : PrepareFirstFactorParams

  @AutoMap
  @Serializable
  data class ResetPasswordEmailCode(
    @SerialName("email_address_id") val emailAddressId: String,
    override val strategy: String = "reset_password_email_code",
  ) : PrepareFirstFactorParams

  @AutoMap
  @Serializable
  data class ResetPasswordPhoneCode(
    @SerialName("phone_number_id") val phoneNumberId: String,
    override val strategy: String = "reset_password_phone_code",
  ) : PrepareFirstFactorParams

  @AutoMap
  @Serializable
  data class Unknown(override val strategy: String = "unknown") : PrepareFirstFactorParams

  companion object {
    fun fromStrategy(
      signIn: SignIn,
      strategy: RequestParams.SignInRequest.PrepareFirstFactor,
    ): PrepareFirstFactorParams {
      val firstFactors = signIn.supportedFirstFactors ?: return Unknown()
      val emailId = firstFactors.find { it.strategy == "email_code" }?.emailAddressId
      val phoneId = firstFactors.find { it.strategy == "phone_code" }?.phoneNumberId

      return when (strategy) {
        RequestParams.SignInRequest.PrepareFirstFactor.RESET_PASSWORD_EMAIL_CODE ->
          ResetPasswordEmailCode(emailId.orEmpty())

        RequestParams.SignInRequest.PrepareFirstFactor.EMAIL_CODE -> {
          val emailId = firstFactors.find { it.strategy == "email_code" }?.emailAddressId
          EmailCode(emailId.orEmpty())
        }

        RequestParams.SignInRequest.PrepareFirstFactor.PHONE_CODE ->
          ResetPasswordPhoneCode(phoneId.orEmpty())
        RequestParams.SignInRequest.PrepareFirstFactor.RESET_PASSWORD_PHONE_CODE -> {
          val phoneId = firstFactors.find { it.strategy == "phone_code" }?.phoneNumberId
          PhoneCode(phoneId.orEmpty())
        }

        else -> Unknown()
      }
    }
  }
}
