package com.clerk.ui.reverification

import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import org.junit.Assert.assertEquals
import org.junit.Test

class ReverificationFactorsTest {
  private val password = Factor(strategy = Strategy.Password.value)
  private val passkey = Factor(strategy = Strategy.Passkey.value)
  private val biometric = Factor(strategy = Strategy.TrustedDevice.value)
  private val email = Factor(strategy = Strategy.EmailCode.value, safeIdentifier = "a@clerk.dev")
  private val primaryEmail =
    Factor(strategy = Strategy.EmailCode.value, safeIdentifier = "b@clerk.dev", primary = true)
  private val phone = Factor(strategy = Strategy.PhoneCode.value, safeIdentifier = "+15555550100")
  private val totp = Factor(strategy = Strategy.Totp.value)
  private val backupCode = Factor(strategy = Strategy.BackupCode.value)
  private val resetEmail = Factor(strategy = Strategy.ResetPasswordEmailCode.value)
  private val resetPhone = Factor(strategy = Strategy.ResetPasswordPhoneCode.value)

  @Test
  fun firstFactorsAreOrderedWithPasskeyFirstAndPasswordLast() {
    val usable =
      ReverificationFactors.usable(
        listOf(password, phone, email, resetEmail, passkey, biometric, primaryEmail),
        ReverificationStage.FIRST_FACTOR,
        hasLocalBiometricCredential = true,
      )

    assertEquals(listOf(passkey, biometric, primaryEmail, email, phone, password), usable)
  }

  @Test
  fun secondFactorsAreOrderedWithBackupCodeLast() {
    val usable =
      ReverificationFactors.usable(
        listOf(backupCode, phone, totp, passkey),
        ReverificationStage.SECOND_FACTOR,
        hasLocalBiometricCredential = false,
      )

    assertEquals(listOf(passkey, totp, phone, backupCode), usable)
  }

  @Test
  fun biometricsNeedALocalCredential() {
    val usable =
      ReverificationFactors.usable(
        listOf(biometric, password),
        ReverificationStage.FIRST_FACTOR,
        hasLocalBiometricCredential = false,
      )

    assertEquals(listOf(password), usable)
  }

  @Test
  fun codeFactorsNeedAnIdentifier() {
    val usable =
      ReverificationFactors.usable(
        listOf(Factor(strategy = Strategy.EmailCode.value), phone),
        ReverificationStage.FIRST_FACTOR,
        hasLocalBiometricCredential = false,
      )

    assertEquals(listOf(phone), usable)
  }

  @Test
  fun stageSpecificStrategiesStayInTheirStage() {
    assertEquals(
      emptyList<Factor>(),
      ReverificationFactors.usable(
        listOf(totp, backupCode),
        ReverificationStage.FIRST_FACTOR,
        hasLocalBiometricCredential = false,
      ),
    )
    assertEquals(
      emptyList<Factor>(),
      ReverificationFactors.usable(
        listOf(password, email),
        ReverificationStage.SECOND_FACTOR,
        hasLocalBiometricCredential = false,
      ),
    )
  }

  @Test
  fun theResetPasswordFactorPrefersEmail() {
    assertEquals(
      resetEmail,
      ReverificationFactors.resetPasswordFactor(listOf(resetPhone, password, resetEmail)),
    )
    assertEquals(resetPhone, ReverificationFactors.resetPasswordFactor(listOf(resetPhone)))
  }

  @Test
  fun anInstanceThatPrefersPasswordStartsWithPasswordAfterPasskeys() {
    val usable = listOf(email, phone, password)

    assertEquals(
      password,
      ReverificationFactors.startingFactor(usable, ReverificationStage.FIRST_FACTOR, true),
    )
    assertEquals(
      email,
      ReverificationFactors.startingFactor(usable, ReverificationStage.FIRST_FACTOR, false),
    )
    assertEquals(
      passkey,
      ReverificationFactors.startingFactor(
        listOf(passkey, password),
        ReverificationStage.FIRST_FACTOR,
        true,
      ),
    )
  }

  @Test
  fun preferringPasswordDoesNotChangeTheSecondFactorStart() {
    assertEquals(
      totp,
      ReverificationFactors.startingFactor(
        listOf(totp, backupCode),
        ReverificationStage.SECOND_FACTOR,
        true,
      ),
    )
  }
}
