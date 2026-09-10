package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleIdentityRequestTest {
  @Test
  fun requestsFreshNonceAndReturnsDecodedIdentityToken() = runBlocking {
    val nonces = mutableSetOf<String>()
    repeat(2) {
      val result =
        AndroidGoogleIdentity.get(
          buildJsonObject { put("clientId", "configured_google_client") }
        ) { request ->
          val option = request.credentialOptions.single() as GetGoogleIdOption
          check(option.serverClientId == "configured_google_client")
          check(!option.filterByAuthorizedAccounts && option.autoSelectEnabled)
          val nonce = checkNotNull(option.nonce)
          UUID.fromString(nonce)
          check(nonces.add(nonce))
          GoogleIdTokenCredential.Builder()
            .setId("user@example.com")
            .setIdToken(googleIdentityTokenFixture)
            .build()
        }
      check(result == buildJsonObject { put("token", googleIdentityTokenFixture) })
    }
  }

  @Test
  fun blankClientIdentifierFailsBeforeCredentialRequest() = runBlocking {
    for (client in listOf("", "  ")) {
      val error =
        runCatching {
            AndroidGoogleIdentity.get(buildJsonObject { put("clientId", client) }) {
              error("Unexpected credential request")
            }
          }
          .exceptionOrNull()
      check(error is CoreException && error.code == "invalid_credential_options")
    }
  }

  @Test
  fun missingActivityDoesNotAdvertiseOrPresentGoogleIdentity() = runBlocking {
    val host =
      AndroidCapabilities(
        publishableKey = "fixture",
        frontendAPI = "https://fixture.invalid",
        storage =
          object : CredentialStorage {
            override suspend fun read(): String? = error("Unexpected storage")

            override suspend fun write(value: String) {
              error("Unexpected storage")
            }

            override suspend fun remove() {
              error("Unexpected storage")
            }
          },
      )
    check("googleIdentity" !in host.supported)
    val error =
      runCatching {
          host.perform(
            "googleIdentity",
            buildJsonObject { put("clientId", "configured_google_client") },
          )
        }
        .exceptionOrNull()
    check(error is CoreException && error.code == "presentation_unavailable")
  }
}
