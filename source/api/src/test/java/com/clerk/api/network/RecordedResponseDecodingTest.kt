package com.clerk.api.network

import com.clerk.api.billing.BillingCreditLedger
import com.clerk.api.network.model.account.EnterpriseAccount
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.Role
import java.io.File
import kotlinx.serialization.KSerializer
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordedResponseDecodingTest {

  private val serializers: Map<String, KSerializer<*>> =
    mapOf(
      "credit-ledger" to BillingCreditLedger.serializer(),
      "enterprise-account" to EnterpriseAccount.serializer(),
      "organization-invitations-page" to
        ClerkPaginatedResponse.serializer(OrganizationInvitation.serializer()),
      "roles-page" to ClerkPaginatedResponse.serializer(Role.serializer()),
    )

  @Test
  fun `every recorded response decodes with the SDK serializers`() {
    val root = File(checkNotNull(javaClass.getResource("/fapi")).toURI())
    val failures =
      root.listFiles().orEmpty().sorted().flatMap { dir ->
        val serializer = serializers[dir.name]
        dir.listFiles().orEmpty().sorted().mapNotNull { fixture ->
          val name = "${dir.name}/${fixture.name}"
          when {
            serializer == null -> "$name: no serializer registered for fapi/${dir.name}"
            else ->
              runCatching { ClerkApi.json.decodeFromString(serializer, fixture.readText()) }
                .exceptionOrNull()
                ?.let { "$name: ${it.message?.lineSequence()?.first()}" }
          }
        }
      }

    assertEquals("Recorded FAPI responses that no longer decode", emptyList<String>(), failures)
  }
}
