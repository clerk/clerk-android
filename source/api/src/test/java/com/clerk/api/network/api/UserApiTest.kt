package com.clerk.api.network.api

import com.clerk.api.network.ApiParams
import kotlin.test.Test
import kotlin.test.assertTrue
import retrofit2.http.Query

class UserApiTest {

  @Test
  fun `passkey endpoints include clerk session id query`() {
    val passkeyMethods =
      listOf(
        "createPasskey",
        "getPasskey",
        "deletePasskey",
        "updatePasskey",
        "attemptPasskeyVerification",
      )

    passkeyMethods.forEach { methodName ->
      assertTrue(
        method(methodName).hasQuery(ApiParams.CLERK_SESSION_ID),
        "$methodName should include _clerk_session_id",
      )
    }
  }

  @Test
  fun `external account linking endpoints include clerk session id query`() {
    val linkingMethods = listOf("createExternalAccount", "reauthorizeExternalAccount")

    linkingMethods.forEach { methodName ->
      assertTrue(
        method(methodName).hasQuery(ApiParams.CLERK_SESSION_ID),
        "$methodName should include _clerk_session_id",
      )
    }
  }

  private fun method(name: String): java.lang.reflect.Method {
    return UserApi::class.java.methods.single { it.name == name }
  }

  private fun java.lang.reflect.Method.hasQuery(value: String): Boolean {
    return parameterAnnotations.any { annotations ->
      annotations.filterIsInstance<Query>().any { it.value == value }
    }
  }
}
