package com.clerk.oauth

import com.clerk.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkApiResult
import com.clerk.signin.SignIn

object GoogleSignInService {

  fun signInWithGoogle(): ClerkApiResult<SignIn, ClerkErrorResponse> {
    return ClerkApiResult.apiFailure(error("Not implemented yet"))
  }
}
