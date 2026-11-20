package com.clerk.api.contract

import com.clerk.api.Constants
import com.clerk.api.network.serialization.UnknownFallbackEnumSerializer
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Test

class SpecContractTest {
  private val spec = SpecSchemas.load()

  @Test
  fun `every mapped model matches the spec except for baseline lines`() {
    val actual =
      ContractMappings.mapped
        .flatMap { ContractDiff.findings(it.serializer.descriptor, spec.properties(it.schema)) }
        .map { it.line }
        .toSortedSet()
    if (System.getProperty("updateContractBaseline") == "true") {
      BASELINE.writeText(actual.joinToString(separator = "\n", postfix = "\n"))
      return
    }
    assertTrue(
      BASELINE.isFile,
      "Missing ${BASELINE.absolutePath}. Run with -PupdateContractBaseline to create it.",
    )
    val baseline = BASELINE.readLines().filter { it.isNotBlank() }.toSet()
    val newDrift = actual - baseline
    val stale = baseline - actual
    assertTrue(
      newDrift.isEmpty() && stale.isEmpty(),
      buildString {
        appendLine("The models and the FAPI spec disagree in ways the baseline does not list.")
        newDrift.forEach { appendLine("new drift: $it") }
        stale.forEach { appendLine("stale baseline line: $it") }
        append("Fix the model, or rerun with -PupdateContractBaseline and review the diff of ")
        append(BASELINE.path)
      },
    )
  }

  @Test
  fun `the pinned spec is the version the SDK sends`() {
    assertEquals(
      Constants.Http.CURRENT_API_VERSION,
      spec.apiVersion,
      "Bump CURRENT_API_VERSION and the pinned spec in ${SpecSchemas.RESOURCE} together",
    )
  }

  @Test
  fun `every serializable class in the public API is mapped or explains why not`() {
    assertTrue(API_DUMP.isFile, "Missing ${API_DUMP.absolutePath}")
    val dump = API_DUMP.readText()
    val serializable =
      Regex("public final synthetic class (\\S+)" + Regex.escape("\$\$serializer"))
        .findAll(dump)
        .map { it.groupValues[1] }
        .toSet()
    val mapped =
      ContractMappings.mapped.map { mapping ->
        val serialName = mapping.serializer.descriptor.serialName
        val type = ContractDiff.jvmClass(serialName)
        requireNotNull(type) { "No class for serial name $serialName" }
        type.name.replace('.', '/')
      }
    val unmapped = ContractMappings.unmapped.map { it.model.java.name.replace('.', '/') }

    val uncovered = serializable - mapped.toSet() - unmapped.toSet()
    assertTrue(
      uncovered.isEmpty(),
      "Add these classes to ContractMappings.mapped, or to unmapped with a reason: $uncovered",
    )
    val unmappedNotInDump = unmapped.toSet() - serializable
    assertTrue(
      unmappedNotInDump.isEmpty(),
      "ContractMappings.unmapped lists classes with no serializer in api.api: $unmappedNotInDump",
    )
    val publicMappedNotInDump =
      (mapped.toSet() - serializable).filter {
        dump.contains(Regex("class ${Regex.escape(it)}[ {]"))
      }
    assertTrue(
      publicMappedNotInDump.isEmpty(),
      "ContractMappings.mapped lists public classes with no serializer in api.api: " +
        publicMappedNotInDump,
    )
    val both = mapped.toSet() intersect unmapped.toSet()
    assertTrue(both.isEmpty(), "Classes both mapped and unmapped: $both")
    val duplicates = (mapped + unmapped).groupingBy { it }.eachCount().filterValues { it > 1 }
    assertTrue(duplicates.isEmpty(), "Classes listed more than once: ${duplicates.keys}")
  }

  @Test
  fun `diff reports a spec property the model does not read`() {
    val findings = diff(mapOf("id" to required(), "email" to optional()))

    assertTrue("contract.Probe|missing-in-sdk|email" in findings)
  }

  @Test
  fun `diff reports a model property the spec does not declare`() {
    val findings = diff(mapOf("id" to required()))

    assertTrue("contract.Probe|missing-in-spec|nick_name" in findings)
    assertFalse(findings.any { it.endsWith("|id") })
  }

  @Test
  fun `diff reports a property the model requires and the spec does not`() {
    val absent = diff(mapOf("id" to required(), "count" to optional()))
    val nullable = diff(mapOf("id" to required(nullable = true), "count" to required()))
    val excusedByDefault = diff(mapOf("id" to required(), "nick_name" to optional(nullable = true)))

    assertTrue("contract.Probe|required-only-in-sdk|count" in absent)
    assertTrue("contract.Probe|required-only-in-sdk|id" in nullable)
    assertFalse(excusedByDefault.any { "required-only-in-sdk" in it })
  }

  @Test
  fun `diff reports spec enum values the model enum lacks`() {
    val findings =
      diff(
        mapOf(
          "id" to required(enumValues = setOf("a", "b")),
          "color" to optional(enumValues = setOf("red", "green")),
          "colors" to optional(enumValues = setOf("blue", "purple")),
          "shade" to optional(enumValues = setOf("light", "dark")),
        )
      )

    assertEquals(
      listOf(
        "contract.Probe|enum-value-missing-in-sdk|color=green",
        "contract.Probe|enum-value-missing-in-sdk|colors=purple",
        "contract.Probe|enum-value-missing-in-sdk|shade=dark",
      ),
      findings.filter { "enum-value" in it },
    )
  }

  @Test
  fun `schemas merge allOf members`() {
    val session = spec.properties("Client.Session")

    assertTrue("expire_at" in session, "expire_at comes from Client.SessionBase")
    assertTrue("last_active_organization_id" in session)
    assertFalse("object" in session)
  }

  @Test
  fun `schemas merge oneOf members into a union`() {
    val verification =
      spec.properties(SchemaRef.PropertyOf("Client.SignIn", "first_factor_verification"))

    assertEquals(
      setOf("unverified", "verified", "failed", "expired", "transferable"),
      verification.getValue("status").enumValues,
      "status unions the enums of every verification schema",
    )
    val strategy = verification.getValue("strategy")
    assertEquals(
      emptySet(),
      strategy.enumValues,
      "Stubs.Verification.Oauth allows any oauth_ strategy, so the union of strategies is open",
    )
    assertTrue(strategy.required)
    assertFalse(
      verification.getValue("expire_at").required,
      "only some verification schemas require expire_at",
    )
    assertTrue("nonce" in verification)
  }

  @Test
  fun `schemas reject an unknown name`() {
    val error = runCatching { spec.properties("Client.Nope") }.exceptionOrNull()

    assertTrue(error?.message.orEmpty().contains("Client.Nope"))
  }

  private fun diff(spec: Map<String, SpecProperty>): List<String> =
    ContractDiff.findings(Probe.serializer().descriptor, spec).map { it.line }

  private fun required(nullable: Boolean = false, enumValues: Set<String> = emptySet()) =
    SpecProperty(nullable = nullable, required = true, enumValues = enumValues)

  private fun optional(nullable: Boolean = false, enumValues: Set<String> = emptySet()) =
    SpecProperty(nullable = nullable, required = false, enumValues = enumValues)

  private companion object {
    val BASELINE = File("src/test/resources/fapi/contract-baseline.txt")
    val API_DUMP = File("api/api.api")
  }
}

@Serializable
internal data class Probe(
  val id: String,
  val nickName: String = "",
  val count: Int,
  val color: Color = Color.RED,
  val colors: List<Color> = emptyList(),
  val shade: Shade = Shade.UNKNOWN,
)

@Serializable
internal enum class Color {
  @SerialName("red") RED,
  @SerialName("blue") BLUE,
}

@OptIn(ExperimentalSerializationApi::class)
@KeepGeneratedSerializer
@Serializable(with = Shade.Serializer::class)
internal enum class Shade {
  @SerialName("light") LIGHT,
  UNKNOWN;

  internal object Serializer : UnknownFallbackEnumSerializer<Shade>(generatedSerializer(), UNKNOWN)
}
