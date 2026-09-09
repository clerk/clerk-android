package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactResourceTest {
  @Test fun deletionUpdatesTheUserAndPreservesReadableHeldContacts() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      var client = base.fixtures.getValue("authenticatedClient").jsonObject
      var session = client.getValue("sessions").jsonArray[0].jsonObject
      val verification = Json.parseToJsonElement("""{"status":"verified","strategy":"email_code","attempts":null,"expire_at":null,"error":null,"verified_at_client":null}""")
      val contacts = mapOf(
        "email_addresses" to """{"object":"email_address","id":"email_contact","email_address":"contact@example.com","matches_sso_connection":false,"linked_to":[]}""",
        "phone_numbers" to """{"object":"phone_number","id":"phone_contact","phone_number":"+15555550123","reserved_for_second_factor":false,"default_second_factor":false,"linked_to":[]}""",
        "external_accounts" to """{"object":"external_account","id":"account_contact","provider":"google","identification_id":"idn_contact","provider_user_id":"provider_contact","approved_scopes":"profile","email_address":"provider@example.com","first_name":"Test","last_name":"User","image_url":"","public_metadata":{}}"""
      ).mapValues { (_, value) -> JsonArray(listOf(JsonObject(Json.parseToJsonElement(value).jsonObject + ("verification" to verification)))) }
      val user = JsonObject(session.getValue("user").jsonObject + contacts)
      session = JsonObject(session + ("user" to user))
      client = JsonObject(client + ("sessions" to JsonArray(listOf(session))))
      base.clientResponse = client
      val deletedCollections = mutableListOf<String>()
      val host = object : NativeCapabilities {
        override val supported = base.supported
        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          if (capability != "http") return base.perform(capability, arguments)
          val args = arguments.jsonObject
          val url = URI(args.getValue("url").requireString())
          val method = if (url.rawQuery?.split('&')?.contains("_method=DELETE") == true) "DELETE" else args.getValue("method").requireString()
          if (method != "DELETE" || !url.path.startsWith("/v1/me/")) return base.perform(capability, arguments)
          val parts = url.path.split('/')
          val collection = parts[parts.size - 2]
          check(collection in contacts)
          val currentSession = client.getValue("sessions").jsonArray[0].jsonObject
          val currentUser = currentSession.getValue("user").jsonObject
          val item = currentUser.getValue(collection).jsonArray[0].jsonObject
          check(item.getValue("id").requireString() == parts.last())
          val updatedUser = JsonObject(currentUser + (collection to JsonArray(emptyList())))
          client = JsonObject(client + ("sessions" to JsonArray(listOf(JsonObject(currentSession + ("user" to updatedUser))))))
          base.clientResponse = client
          deletedCollections += collection
          val receipt = buildJsonObject { put("object", item.getValue("object")); put("id", item.getValue("id")); put("deleted", true) }
          return buildJsonObject {
            put("status", 200)
            put("headers", buildJsonObject { put("authorization", "fixture-client-credential") })
            put("body", buildJsonObject { put("response", receipt); put("client", client) }.toString())
          }
        }
      }
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      try {
        val currentUser = checkNotNull(clerk.user)
        val email = currentUser.emailAddresses.single()
        val phone = currentUser.phoneNumbers.single()
        val account = currentUser.externalAccounts.single()
        email.destroy()
        check(currentUser.emailAddresses.isEmpty() && email.emailAddress == "contact@example.com")
        phone.destroy()
        check(currentUser.phoneNumbers.isEmpty() && phone.phoneNumber == "+15555550123")
        account.destroy()
        check(currentUser.externalAccounts.isEmpty() && account.emailAddress == "provider@example.com")
        check(account.provider.rawValue == "google")
        check(deletedCollections == listOf("email_addresses", "phone_numbers", "external_accounts"))
        clerk.signIn.reset()
        check(clerk.loaded && clerk.user?.id == currentUser.id)
      } finally { clerk.close() }
    }
  }
}
