package com.clerk.ui.auth

import com.clerk.api.*
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricFactorRoutingTest {
  @Test fun deviceFactorsUseTheDedicatedBiometricEntry() {
    val signIn = mockk<SignIn>()
    every { signIn.supportedFirstFactors } returns listOf(
      SignInFirstFactor.Case11(TrustedDeviceFactor(Field.Value("tdc_other_device"), Field.Value("Another device"))),
      SignInFirstFactor.Case3(PhoneCodeFactor("idn_phone", "+15555550123")),
    )
    val choices = signIn.firstFactorChoices
    assertEquals(listOf("phone_code"), choices.map { it.strategy })
    assertEquals("idn_phone", choices.single().phoneNumberId)
    assertEquals(listOf("phone_code"), signIn.alternativeFirstFactors().map { it.strategy })
  }
}
