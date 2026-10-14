package com.clerk.api.sso

import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class OAuthResultTest {

  private val signIn = SignIn(id = "sia_1", status = SignIn.Status.COMPLETE)
  private val signUp = mockk<SignUp>()

  @Test
  fun `outcome carries the sign in`() {
    assertEquals(OAuthResult.Outcome.SignIn(signIn), OAuthResult(signIn = signIn).outcome)
  }

  @Test
  fun `outcome carries the sign up`() {
    assertEquals(OAuthResult.Outcome.SignUp(signUp), OAuthResult(signUp = signUp).outcome)
  }

  @Test
  fun `outcome prefers the sign in when both are present`() {
    assertEquals(
      OAuthResult.Outcome.SignIn(signIn),
      OAuthResult(signIn = signIn, signUp = signUp).outcome,
    )
  }

  @Test
  fun `outcome is Empty when neither is present`() {
    assertEquals(OAuthResult.Outcome.Empty, OAuthResult().outcome)
  }
}
