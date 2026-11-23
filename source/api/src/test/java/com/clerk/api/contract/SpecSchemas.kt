package com.clerk.api.contract

import java.security.MessageDigest
import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

internal data class SpecScalar(val type: String, val format: String?, val array: Boolean = false) {
  override fun toString(): String {
    val item = listOfNotNull(type, format).joinToString("/")
    return if (array) "array<$item>" else item
  }
}

internal data class SpecProperty(
  val nullable: Boolean,
  val required: Boolean,
  val enumValues: Set<String>?,
  val scalar: SpecScalar? = null,
)

internal sealed interface SchemaRef {
  data class Named(val schema: String) : SchemaRef {
    override fun toString(): String = schema
  }

  data class PropertyOf(val schema: String, val path: List<String>) : SchemaRef {
    constructor(schema: String, vararg path: String) : this(schema, path.toList())

    override fun toString(): String = (listOf(schema) + path).joinToString(".")
  }
}

internal class SpecSchemas
private constructor(
  private val schemas: Map<String, Node>,
  val apiVersion: String,
  val sha256: String,
) {

  fun properties(schema: String): Map<String, SpecProperty> = properties(SchemaRef.Named(schema))

  fun properties(ref: SchemaRef): Map<String, SpecProperty> {
    val flat =
      when (ref) {
        is SchemaRef.Named -> flatten(named(ref.schema), emptySet())
        is SchemaRef.PropertyOf -> flatten(propertyNode(ref), emptySet())
      }
    require(flat != null && flat.properties.isNotEmpty()) {
      "$ref has no object properties in the spec"
    }
    return flat.properties
      .filterKeys { it != "object" }
      .mapValues { (key, merged) ->
        SpecProperty(merged.nullable, key in flat.required, merged.enumValues, merged.scalar)
      }
  }

  private fun named(schema: String): Node =
    requireNotNull(schemas[schema]) { "The spec has no schema named '$schema'" }

  private fun propertyNode(ref: SchemaRef.PropertyOf): Node =
    ref.path.fold(named(ref.schema)) { owner, property ->
      val merged =
        requireNotNull(flatten(owner, emptySet())?.properties?.get(property)) {
          "$ref: no property '$property' in the spec"
        }
      merged.node.child("items") ?: merged.node
    }

  private fun flatten(node: Node, seen: Set<String>): Flat? {
    val name = node.refName()
    return when {
      name == null -> flattenComposite(node, seen)
      name in seen -> null
      else -> flatten(named(name), seen + name)
    }
  }

  private fun flattenComposite(node: Node, seen: Set<String>): Flat? {
    val builder = FlatBuilder()
    node.children("allOf").mapNotNull { flatten(it, seen) }.forEach(builder::addAllOfMember)
    val alternatives = node.children("oneOf").ifEmpty { node.children("anyOf") }
    if (alternatives.isNotEmpty()) {
      val flats = alternatives.map { flatten(it, seen) }
      if (flats.any { it == null }) return null
      builder.addAlternatives(flats.filterNotNull())
    }
    node.child("properties")?.let { declared ->
      builder.addDeclared(declared.entries().associate { (key, value) -> key to shape(value) })
    }
    builder.addRequired(node.strings("required"))
    return builder.build()
  }

  private fun shape(node: Node): Merged =
    Merged(node, node.flag("nullable"), enumValues(node), scalarOf(node))

  private fun enumValues(node: Node, seen: Set<String> = emptySet()): Set<String>? {
    val name = node.refName()
    val items = node.child("items")
    return when {
      name != null -> if (name in seen) null else enumValues(named(name), seen + name)
      node.strings("enum").isNotEmpty() -> node.strings("enum").toSet()
      items != null -> enumValues(items, seen)
      else ->
        node
          .children("allOf")
          .map { enumValues(it, seen) }
          .fold(null as Set<String>?) { all, member -> intersect(all, member) }
    }
  }

  private fun scalarOf(node: Node, seen: Set<String> = emptySet()): SpecScalar? {
    val name = node.refName()
    val type = node.string("type")
    return when {
      name != null -> if (name in seen) null else scalarOf(named(name), seen + name)
      type == "array" -> node.child("items")?.let { scalarOf(it, seen) }?.copy(array = true)
      type != null && type in SCALAR_TYPES -> SpecScalar(type, node.string("format"))
      else -> node.children("allOf").firstNotNullOfOrNull { scalarOf(it, seen) }
    }
  }

  private inner class FlatBuilder {
    val properties = linkedMapOf<String, Merged>()
    val required = mutableSetOf<String>()
    private var found = false

    fun addAllOfMember(flat: Flat) {
      absorb(flat.properties)
      required += flat.required
    }

    fun addAlternatives(flats: List<Flat>) {
      val union = linkedMapOf<String, Merged>()
      flats.forEach { flat ->
        flat.properties.forEach { (key, value) ->
          union[key] = union[key]?.let { eitherOf(it, value) } ?: value
        }
      }
      absorb(union)
      required += flats.map { it.required }.reduce { shared, next -> shared intersect next }
    }

    fun addDeclared(declared: Map<String, Merged>) {
      found = true
      declared.forEach { (key, value) ->
        properties[key] = properties[key]?.let { bothOf(value, it) } ?: value
      }
    }

    fun addRequired(names: List<String>) {
      if (names.isEmpty()) return
      found = true
      required += names
    }

    fun build(): Flat? = if (found) Flat(properties, required) else null

    private fun absorb(members: Map<String, Merged>) {
      found = true
      members.forEach { (key, value) ->
        properties[key] = properties[key]?.let { bothOf(it, value) } ?: value
      }
    }
  }

  private fun bothOf(a: Merged, b: Merged): Merged =
    Merged(
      node = a.node,
      nullable = a.nullable || b.nullable,
      enumValues = intersect(a.enumValues, b.enumValues),
      scalar = a.scalar ?: b.scalar,
    )

  private fun eitherOf(a: Merged, b: Merged): Merged =
    Merged(
      node = a.node,
      nullable = a.nullable || b.nullable,
      enumValues =
        if (a.enumValues != null && b.enumValues != null) a.enumValues + b.enumValues else null,
      scalar = a.scalar.takeIf { it == b.scalar },
    )

  private fun intersect(a: Set<String>?, b: Set<String>?): Set<String>? =
    when {
      a == null -> b
      b == null -> a
      else -> a intersect b
    }

  private class Flat(val properties: Map<String, Merged>, val required: Set<String>)

  private class Merged(
    val node: Node,
    val nullable: Boolean,
    val enumValues: Set<String>?,
    val scalar: SpecScalar?,
  )

  @JvmInline
  private value class Node(val map: Map<*, *>) {
    fun child(key: String): Node? = (map[key] as? Map<*, *>)?.let(::Node)

    fun children(key: String): List<Node> =
      (map[key] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>().map(::Node)

    fun strings(key: String): List<String> =
      (map[key] as? List<*>).orEmpty().filterIsInstance<String>()

    fun string(key: String): String? = map[key] as? String

    fun flag(key: String): Boolean = map[key] == true

    fun entries(): List<Pair<String, Node>> =
      map.entries.mapNotNull { (key, value) ->
        (value as? Map<*, *>)?.let { key.toString() to Node(it) }
      }

    fun refName(): String? = (map["\$ref"] as? String)?.substringAfterLast('/')
  }

  companion object {
    const val RESOURCE = "fapi/fapi-2026-08-20.yml"

    val UPSTREAM_SHA256: Map<String, String> =
      mapOf("2026-08-20" to "4561b6cd7ef96827b173b7fc8432d56bbd7c7e023502f7df989c996ee9aac36e")

    private val SCALAR_TYPES = setOf("string", "integer", "number", "boolean")

    fun load(): SpecSchemas {
      val stream =
        requireNotNull(SpecSchemas::class.java.classLoader?.getResourceAsStream(RESOURCE)) {
          "Test resource $RESOURCE is missing"
        }
      val version =
        requireNotNull(Regex("""\d{4}-\d{2}-\d{2}""").find(RESOURCE)?.value) {
          "$RESOURCE has no date in its name"
        }
      return parse(stream.use { it.readBytes() }, version)
    }

    fun parse(bytes: ByteArray, apiVersion: String): SpecSchemas {
      val document = Load(LoadSettings.builder().build()).loadFromInputStream(bytes.inputStream())
      val schemas =
        requireNotNull(Node(document as Map<*, *>).child("components")?.child("schemas")) {
          "The spec has no components.schemas"
        }
      val sha256 =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
      return SpecSchemas(schemas.entries().toMap(), apiVersion, sha256)
    }
  }
}
