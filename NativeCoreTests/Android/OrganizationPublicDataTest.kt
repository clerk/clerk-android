package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizationPublicDataTest {
  @Test fun invitationPreservesPresentBranding() = verify(false, "present")

  @Test fun invitationDefaultsNullBranding() = verify(false, "null")

  @Test fun invitationDefaultsOmittedBranding() = verify(false, "omitted")

  @Test fun invitationDefaultsNullImageAndOmittedSlug() = verify(false, "mixed")

  @Test fun suggestionPreservesPresentBranding() = verify(true, "present")

  @Test fun suggestionDefaultsNullBranding() = verify(true, "null")

  @Test fun suggestionDefaultsOmittedBranding() = verify(true, "omitted")

  @Test fun suggestionDefaultsNullImageAndOmittedSlug() = verify(true, "mixed")

  private fun verify(suggestion: Boolean, scenario: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        base.clientResponse = base.fixtures.getValue("authenticatedClient")
        val kind = if (suggestion) "suggestion" else "invitation"
        val metadata =
          Json.parseToJsonElement("""{"source":"test","nested":{"retained":null},"count":3}""")
        val payload = buildJsonObject {
          put("object", "organization_$kind")
          put("id", "resource_projection")
          put("email_address", "sam@example.com")
          put("role", "org:member")
          put("status", "pending")
          put("public_metadata", metadata)
          put("created_at", 1713200000000L)
          put("updated_at", "2024-04-15T19:17:53Z")
          put(
            "public_organization_data",
            buildJsonObject {
              put("id", "org_123")
              put("name", "Acme")
              put("has_image", scenario == "present")
              if (scenario == "present") {
                put("image_url", "https://fixture.test/logo.png")
                put("slug", "acme")
              }
              if (scenario == "null" || scenario == "mixed") put("image_url", JsonNull)
              if (scenario == "null") put("slug", JsonNull)
            },
          )
        }
        var reads = 0
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              val args = arguments.jsonObject
              if (
                capability == "http" &&
                  Uri.parse(args.getValue("url").requireString()).path ==
                    "/v1/me/organization_${kind}s"
              ) {
                reads++
                check(args["method"] == JsonPrimitive("GET"))
                check(
                  Uri.parse(args.getValue("url").requireString())
                    .getQueryParameter("_clerk_session_id") == "sess_native"
                )
                return buildJsonObject {
                  put("status", 200)
                  put("headers", buildJsonObject {})
                  put(
                    "body",
                    buildJsonObject {
                        put(
                          "response",
                          buildJsonObject {
                            put("data", JsonArray(listOf(payload)))
                            put("total_count", 1)
                          },
                        )
                      }
                      .toString(),
                  )
                }
              }
              return base.perform(capability, arguments)
            }
          }
        val clerk =
          Clerk.connect(
            instrumentation.targetContext,
            ClerkConfiguration(
              "pk_test_" +
                Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray()),
              "clerk-test://sso-callback",
            ),
            host,
          )
        try {
          val user = checkNotNull(clerk.user)
          val session = clerk.session
          val data: JsonObject
          val created: Instant
          val updated: Instant
          if (suggestion) {
            val page = user.getOrganizationSuggestions()
            check(page.totalCount == 1.0)
            val resource = page.data.single()
            check(resource.id == "resource_projection" && resource.status.rawValue == "pending")
            data = resource.publicOrganizationData.toJson().jsonObject
            created = resource.createdAt
            updated = resource.updatedAt
          } else {
            val page = user.getOrganizationInvitations()
            check(page.totalCount == 1.0)
            val resource = page.data.single()
            check(resource.id == "resource_projection" && resource.status.rawValue == "pending")
            check(resource.publicMetadata == metadata)
            data = resource.publicOrganizationData.toJson().jsonObject
            created = resource.createdAt
            updated = resource.updatedAt
          }
          check(
            data.getValue("imageUrl").requireString() ==
              if (scenario == "present") "https://fixture.test/logo.png" else ""
          )
          check(data["slug"] == if (scenario == "present") JsonPrimitive("acme") else JsonNull)
          check(data.getValue("hasImage").jsonPrimitive.boolean == (scenario == "present"))
          check(data["id"] == JsonPrimitive("org_123") && data["name"] == JsonPrimitive("Acme"))
          check(
            created.toEpochMilli() == 1713200000000L && updated.toEpochMilli() == 1713208673000L
          )
          check(reads == 1 && clerk.user === user && clerk.session === session)
        } finally {
          clerk.close()
        }
      }
    }
  }
}
