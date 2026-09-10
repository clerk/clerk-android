package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OAuthProviderRequestTest {
  @Test
  fun customExternalAccountPreservesStrategyThroughBrowserAndReload() = verify("custom_patreon")

  @Test fun googleExternalAccountUsesCanonicalStrategyAndReturnedResource() = verify("google")

  private fun verify(provider: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        val callback = "clerk-test://sso-callback"
        val authorization = "https://provider.example/authorize"
        val account =
          Json.parseToJsonElement(
              """{"object":"external_account","id":"account_provider","provider":"$provider","identification_id":"idn_provider","provider_user_id":"provider_user","approved_scopes":"profile","email_address":"provider@example.com","first_name":"Test","last_name":"User","image_url":"","public_metadata":{},"verification":{"status":"verified","strategy":"oauth_$provider","attempts":null,"expire_at":null,"error":null,"verified_at_client":null,"external_verification_redirect_url":"$authorization"}}"""
            )
            .jsonObject
        val initialClient = base.fixtures.getValue("authenticatedClient").jsonObject
        val session = initialClient.getValue("sessions").jsonArray.single().jsonObject
        val user = session.getValue("user").jsonObject
        val updatedUser = JsonObject(user + ("external_accounts" to JsonArray(listOf(account))))
        val updatedSession = JsonObject(session + ("user" to updatedUser))
        val updatedClient =
          JsonObject(initialClient + ("sessions" to JsonArray(listOf(updatedSession))))
        base.clientResponse = initialClient
        val requests = mutableListOf<JsonObject>()
        var browserCalls = 0
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              val args = arguments.jsonObject
              if (capability == "browser") {
                browserCalls++
                check(args["url"] == JsonPrimitive(authorization))
                check(args["callbackUrl"] == JsonPrimitive(callback))
                base.clientResponse = updatedClient
                return buildJsonObject {
                  put("callbackUrl", "$callback?rotating_token_nonce=provider_nonce")
                }
              }
              if (capability != "http") return base.perform(capability, arguments)
              requests += args
              val url = Uri.parse(args.getValue("url").requireString())
              if (url.path != "/v1/me/external_accounts") return base.perform(capability, arguments)
              check(args["method"] == JsonPrimitive("POST"))
              return buildJsonObject {
                put("status", 200)
                put("headers", buildJsonObject {})
                put("body", buildJsonObject { put("response", account) }.toString())
              }
            }
          }
        val key =
          "pk_test_" +
            Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
        val clerk =
          Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, callback), host)
        try {
          val owner = checkNotNull(clerk.user)
          val selected = checkNotNull(clerk.session)
          requests.clear()
          val connected =
            owner.createExternalAccount(
              CreateExternalAccountParams(
                strategy =
                  if (provider == "google") CreateExternalAccountParamsStrategy.OauthGoogle
                  else CreateExternalAccountParamsStrategy.Unrecognized("oauth_$provider"),
                additionalScopes = listOf("profile", "memberships"),
                oidcPrompt = "consent",
                oidcLoginHint = "provider@example.com",
              )
            )
          check(
            requests.map { Uri.parse(it.getValue("url").requireString()).path } ==
              listOf("/v1/me/external_accounts", "/v1/client")
          )
          val body =
            Uri.parse("https://fixture.test/?" + requests.first().getValue("body").requireString())
          check(body.getQueryParameter("strategy") == "oauth_$provider")
          check(body.getQueryParameter("redirect_url") == callback)
          check(body.getQueryParameters("additional_scope") == listOf("profile", "memberships"))
          check(body.getQueryParameter("oidc_prompt") == "consent")
          check(body.getQueryParameter("oidc_login_hint") == "provider@example.com")
          val reload = Uri.parse(requests.last().getValue("url").requireString())
          check(
            requests.last()["method"] == JsonPrimitive("GET") &&
              reload.getQueryParameter("rotating_token_nonce") == "provider_nonce"
          )
          check(browserCalls == 1 && clerk.session === selected && clerk.user === owner)
          check(connected === owner.externalAccounts.single())
          check(connected.id == "account_provider" && connected.provider.rawValue == provider)
          check(owner.verifiedExternalAccounts.single() === connected)
        } finally {
          clerk.close()
        }
      }
    }
  }
}
