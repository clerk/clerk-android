package com.clerk.ui.organizationlist

import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clerk.api.*
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizationAccountListCoreTest {
  @Test fun acceptingAnInvitationDoesNotSkipOrDuplicateAPendingRow() = verify(2)

  @Test fun acceptingInvitationsNeverSendsAFractionalOffset() = verify(10, acceptCount = 9)

  private fun verify(pageSize: Int, acceptCount: Int = 1) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val fixtures =
        Json.parseToJsonElement(
            instrumentation.context.assets.open("fapi.json").bufferedReader().use { it.readText() }
          )
          .jsonObject
      val host = OrganizationListHost(fixtures, pageSize + 1)
      val key =
        "pk_test_" +
          Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk =
        try {
          Clerk.connect(
            instrumentation.targetContext,
            ClerkConfiguration(key, "clerk-test://sso-callback"),
            host,
          )
        } catch (error: CoreException) {
          throw AssertionError("Fixture connection failed: ${error.code}", error)
        }
      val model = OrganizationAccountListViewModel(clerk, pageSize, Dispatchers.Main.immediate)
      try {
        model.load()
        val loaded = withTimeout(5000) { model.state.first { !it.isLoading } }
        check(loaded.errorMessage == null) { loaded.errorMessage.orEmpty() }
        check(loaded.invitations.map { it.id } == (1..pageSize).map { "inv_$it" })
        repeat(acceptCount) { index ->
          model.acceptInvitation(model.state.value.invitations[index])
          val updated = withTimeout(5000) { model.state.first { it.activeActionId == null } }
          check(updated.errorMessage == null) { updated.errorMessage.orEmpty() }
        }
        val accepted = model.state.value
        check(accepted.errorMessage == null) { accepted.errorMessage.orEmpty() }
        check(accepted.invitations.first().status == OrganizationInvitationStatus.Accepted)
        check(
          accepted.pendingInvitationsCount == pageSize - acceptCount &&
            accepted.invitationsHasNextPage
        )
        model.loadMoreInvitations()
        val completed = withTimeout(5000) { model.state.first { !it.isLoadingMoreInvitations } }
        check(completed.errorMessage == null) {
          "Pagination failed; offsets=${host.offsets}, error=${completed.errorMessage}"
        }
        check(completed.invitations.map { it.id } == (1..pageSize + 1).map { "inv_$it" })
        check(
          completed.pendingInvitationsCount == pageSize + 1 - acceptCount &&
            !completed.invitationsHasNextPage
        )
        check(completed.invitations.first().publicOrganizationData.id == "org_inv_1")
        check(host.offsets.all { it.toIntOrNull() != null })
      } finally {
        model.viewModelScope.cancel()
        clerk.close()
      }
    }
  }
}

private class OrganizationListHost(private val fixtures: JsonObject, count: Int) :
  NativeCapabilities {
  override val supported = setOf("http", "storage", "timer", "random")
  val offsets = mutableListOf<String>()
  private val invitations = (1..count).map { invitation("inv_$it") }.toMutableList()
  private var credential: JsonElement = JsonNull

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    val args = arguments.jsonObject
    when (capability) {
      "storage.read" -> return credential
      "storage.write" -> {
        credential = args.getValue("value")
        return JsonNull
      }
      "storage.remove" -> {
        credential = JsonNull
        return JsonNull
      }
      "timer" -> {
        delay(args.getValue("milliseconds").jsonPrimitive.double.toLong())
        return JsonNull
      }
    }
    check(capability == "http")
    val url = Uri.parse(args.getValue("url").jsonPrimitive.content)
    val path = checkNotNull(url.path)
    val payload =
      when {
        path.endsWith("/environment") -> fixtures.getValue("environment")
        path.endsWith("/client") -> fixtures.getValue("authenticatedClient")
        path.contains("/organization_invitations/") && path.endsWith("/accept") -> {
          val id = url.pathSegments[url.pathSegments.size - 2]
          val accepted = invitation(id, "accepted")
          invitations[invitations.indexOfFirst { it.getValue("id").jsonPrimitive.content == id }] =
            accepted
          accepted
        }
        path.endsWith("/organization_invitations") -> {
          val rawOffset = url.getQueryParameter("offset") ?: "0"
          offsets += rawOffset
          val offset =
            rawOffset.toIntOrNull()
              ?: return reply(
                buildJsonObject {
                  put(
                    "errors",
                    buildJsonArray {
                      add(
                        buildJsonObject {
                          put("code", "invalid_pagination_offset")
                          put("message", "Offset must be an integer")
                        }
                      )
                    },
                  )
                },
                422,
              )
          val limit = checkNotNull(url.getQueryParameter("limit")?.toIntOrNull())
          val pending = invitations.filter {
            it.getValue("status").jsonPrimitive.content == "pending"
          }
          buildJsonObject {
            put("data", JsonArray(pending.drop(offset).take(limit)))
            put("total_count", pending.size)
          }
        }
        path.endsWith("/organization_memberships") || path.endsWith("/organization_suggestions") ->
          buildJsonObject {
            put("data", buildJsonArray {})
            put("total_count", 0)
          }
        path.endsWith("/organization_creation_defaults") ->
          buildJsonObject {
            put("advisory", JsonNull)
            put(
              "form",
              buildJsonObject {
                put("name", "Suggested")
                put("slug", "suggested")
                put("logo", JsonNull)
                put("blur_hash", JsonNull)
              },
            )
          }
        else -> error("Unexpected request: $path")
      }
    return reply(
      buildJsonObject { put("response", payload) },
      authorize = !path.endsWith("/environment"),
    )
  }

  private fun reply(body: JsonElement, status: Int = 200, authorize: Boolean = true) =
    buildJsonObject {
      put("status", status)
      put(
        "headers",
        buildJsonObject {
          if (status == 200 && authorize) put("authorization", "fixture_client_credential")
        },
      )
      put("body", body.toString())
    }

  private fun invitation(id: String, status: String = "pending") =
    Json.parseToJsonElement(
        """
    {"object":"organization_invitation","id":"$id","email_address":"person@example.com","public_organization_data":{"id":"org_$id","name":"Organization $id","slug":null,"image_url":"","has_image":false},"public_metadata":{},"role":"org:member","status":"$status","created_at":1700000000000,"updated_at":1700000000000}
  """
      )
      .jsonObject
}
