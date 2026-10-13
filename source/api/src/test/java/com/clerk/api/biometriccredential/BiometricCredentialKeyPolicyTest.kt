package com.clerk.api.biometriccredential

import android.security.keystore.KeyProperties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BiometricCredentialKeyPolicyTest {
  @Test
  @Config(sdk = [30])
  fun `strict keys require per use strong biometrics and invalidate on enrollment changes`() {
    val spec =
      DefaultBiometricCredentialKeyManager.keyGenParameterSpec(
        "strict_key",
        BiometricCredentialPolicy.BIOMETRY_CURRENT_SET,
      )

    assertTrue(spec.isUserAuthenticationRequired)
    assertEquals(KeyProperties.AUTH_BIOMETRIC_STRONG, spec.userAuthenticationType)
    assertEquals(0, spec.userAuthenticationValidityDurationSeconds)
    assertTrue(spec.isInvalidatedByBiometricEnrollment)
  }

  @Test
  @Config(sdk = [28])
  fun `strict keys on older Android require per use biometrics and invalidate on enrollment changes`() {
    val spec =
      DefaultBiometricCredentialKeyManager.keyGenParameterSpec(
        "strict_key",
        BiometricCredentialPolicy.BIOMETRY_CURRENT_SET,
      )

    assertTrue(spec.isUserAuthenticationRequired)
    assertEquals(-1, spec.userAuthenticationValidityDurationSeconds)
    assertTrue(spec.isInvalidatedByBiometricEnrollment)
  }

  @Test
  @Config(sdk = [30])
  fun `explicit PIN capable enrollment retains its original key protections`() {
    val spec =
      DefaultBiometricCredentialKeyManager.keyGenParameterSpec(
        "pin_capable_key",
        BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE,
      )

    assertTrue(spec.isUserAuthenticationRequired)
    assertEquals(
      KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
      spec.userAuthenticationType,
    )
    assertEquals(0, spec.userAuthenticationValidityDurationSeconds)
    assertFalse(spec.isInvalidatedByBiometricEnrollment)
  }
}
