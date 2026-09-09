package com.clerk.testing

import com.clerk.api.*
import com.clerk.ui.auth.FactorSelection
import io.mockk.*
import java.time.Instant

fun firstFactor(selection: FactorSelection): SignInFirstFactor =
  when (selection.strategy) {
    "email_code" ->
      SignInFirstFactor.Case1(
        EmailCodeFactor(
          requireNotNull(selection.emailAddressId),
          selection.safeIdentifier.orEmpty(),
          selection.primary,
        )
      )
    "email_link" ->
      SignInFirstFactor.Case2(
        EmailLinkFactor(
          requireNotNull(selection.emailAddressId),
          selection.safeIdentifier.orEmpty(),
          selection.primary,
        )
      )
    "phone_code" ->
      SignInFirstFactor.Case3(
        PhoneCodeFactor(
          requireNotNull(selection.phoneNumberId),
          selection.safeIdentifier.orEmpty(),
          selection.primary,
          selection.default,
        )
      )
    "password" -> SignInFirstFactor.Case5(PasswordFactor())
    "passkey" -> SignInFirstFactor.Case6(PasskeyFactor())
    "enterprise_sso" ->
      SignInFirstFactor.Case8(
        EnterpriseSSOFactor(selection.enterpriseConnectionId, selection.enterpriseConnectionName)
      )
    "reset_password_phone_code" ->
      SignInFirstFactor.Case9(
        ResetPasswordPhoneCodeFactor(
          requireNotNull(selection.phoneNumberId),
          selection.safeIdentifier.orEmpty(),
          selection.primary,
        )
      )
    "reset_password_email_code" ->
      SignInFirstFactor.Case10(
        ResetPasswordEmailCodeFactor(
          requireNotNull(selection.emailAddressId),
          selection.safeIdentifier.orEmpty(),
          selection.primary,
        )
      )
    else -> error("Unsupported fixture strategy ${selection.strategy}")
  }

fun secondFactor(selection: FactorSelection): SignInSecondFactor =
  when (selection.strategy) {
    "email_code" ->
      SignInSecondFactor.Case1(
        EmailCodeFactor(
          requireNotNull(selection.emailAddressId),
          selection.safeIdentifier.orEmpty(),
          selection.primary,
        )
      )
    "email_link" ->
      SignInSecondFactor.Case2(
        EmailLinkFactor(
          requireNotNull(selection.emailAddressId),
          selection.safeIdentifier.orEmpty(),
          selection.primary,
        )
      )
    "phone_code" ->
      SignInSecondFactor.Case3(
        PhoneCodeFactor(
          requireNotNull(selection.phoneNumberId),
          selection.safeIdentifier.orEmpty(),
          selection.primary,
          selection.default,
        )
      )
    "passkey" -> SignInSecondFactor.Case4(PasskeyFactor())
    "totp" -> SignInSecondFactor.Case5(TOTPFactor())
    "backup_code" -> SignInSecondFactor.Case6(BackupCodeFactor())
    else -> error("Unsupported fixture strategy ${selection.strategy}")
  }

fun mockVerification(
  status: VerificationStatus = VerificationStatus.Unverified,
  strategy: String? = null,
  expireAt: Instant? = null,
): Verification {
  val verification = mockk<Verification>(relaxed = true)
  every { verification.status } returns status
  every { verification.strategy } returns strategy
  every { verification.expireAt } returns expireAt
  return verification
}
