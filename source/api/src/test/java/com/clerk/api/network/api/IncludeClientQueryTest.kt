package com.clerk.api.network.api

import com.clerk.api.network.ApiParams
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import retrofit2.http.Query

class IncludeClientQueryTest {

  @Test
  fun `GETs that need the client declare the include client query`() {
    assertTrue(SignInApi::class.java.method("fetchSignIn").hasQuery(ApiParams.INCLUDE_CLIENT))
    assertTrue(SignUpApi::class.java.method("fetchSignUp").hasQuery(ApiParams.INCLUDE_CLIENT))
    assertTrue(UserApi::class.java.method("getUser").hasQuery(ApiParams.INCLUDE_CLIENT))
  }

  @Test
  fun `the client fetch does not ask for the client`() {
    ClientApi::class
      .java
      .methods
      .filter { it.name == "get" || it.name == "getSkippingClientId" }
      .forEach {
        assertFalse(it.hasQuery(ApiParams.INCLUDE_CLIENT), "${it.name} asks for the client")
      }
  }

  @Test
  fun `fetchSignIn asks for the client by default, with or without a rotating token nonce`() =
    runTest {
      val api = mockk<SignInApi>()
      coEvery { api.fetchSignIn(any(), any(), any()) } returns
        ClerkResult.unknownFailure(Exception())

      api.fetchSignIn(id = "sia_1")
      api.fetchSignIn(id = "sia_1", rotatingTokenNonce = "nonce")

      coVerify(exactly = 1) {
        api.fetchSignIn(id = "sia_1", rotatingTokenNonce = null, includeClient = true)
      }
      coVerify(exactly = 1) {
        api.fetchSignIn(id = "sia_1", rotatingTokenNonce = "nonce", includeClient = true)
      }
    }

  @Test
  fun `fetchSignUp asks for the client by default, with or without a rotating token nonce`() =
    runTest {
      val api = mockk<SignUpApi>()
      coEvery { api.fetchSignUp(any(), any(), any()) } returns
        ClerkResult.unknownFailure(Exception())

      api.fetchSignUp(id = "sua_1")
      api.fetchSignUp(id = "sua_1", rotatingTokenNonce = "nonce")

      coVerify(exactly = 1) {
        api.fetchSignUp(id = "sua_1", rotatingTokenNonce = null, includeClient = true)
      }
      coVerify(exactly = 1) {
        api.fetchSignUp(id = "sua_1", rotatingTokenNonce = "nonce", includeClient = true)
      }
    }

  @Test
  fun `getUser asks for the client by default`() = runTest {
    val api = mockk<UserApi>()
    coEvery { api.getUser(any(), any()) } returns ClerkResult.unknownFailure(Exception())

    api.getUser(sessionId = "sess_1")

    coVerify(exactly = 1) { api.getUser(sessionId = "sess_1", includeClient = true) }
  }

  private fun Class<*>.method(name: String): java.lang.reflect.Method = methods.single {
    it.name == name
  }

  private fun java.lang.reflect.Method.hasQuery(value: String): Boolean =
    parameterAnnotations.any { annotations ->
      annotations.filterIsInstance<Query>().any { it.value == value }
    }
}
