package com.clerk.api.contract

import org.snakeyaml.engine.v2.api.Load
import org.snakeyaml.engine.v2.api.LoadSettings

internal data class SpecProperty(
  val nullable: Boolean,
  val required: Boolean,
  val enumValues: Set<String>,
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
private constructor(private val schemas: Map<String, Node>, val apiVersion: String) {

  fun properties(schema: String): Map<String, SpecProperty> = properties(SchemaRef.Named(schema))

  fun properties(ref: SchemaRef): Map<String, SpecProperty> {
    val flat =
      when (ref) {
        is SchemaRef.Named -> flatten(named(ref.schema), emptySet())
        is SchemaRef.PropertyOf -> flatten(propertyNode(ref), emptySet())
      }
    requireNotNull(flat) { "$ref has no object properties in the spec" }
    return flat.properties
      .filterKeys { it != "object" }
      .mapValues { (key, merged) ->
        SpecProperty(merged.nullable, key in flat.required, merged.enumValues)
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
      builder.required += node.strings("required")
    }
    return builder.build()
  }

  private fun shape(node: Node): Merged = Merged(node, node.flag("nullable"), enumValues(node))

  private fun enumValues(node: Node, seen: Set<String> = emptySet()): Set<String> {
    val name = node.refName()
    val items = node.child("items")
    val wrapped = node.children("allOf").singleOrNull()
    return when {
      name != null -> if (name in seen) emptySet() else enumValues(named(name), seen + name)
      node.strings("enum").isNotEmpty() -> node.strings("enum").toSet()
      items != null -> enumValues(items, seen)
      wrapped != null -> enumValues(wrapped, seen)
      else -> emptySet()
    }
  }

  private inner class FlatBuilder {
    val properties = linkedMapOf<String, Merged>()
    val required = mutableSetOf<String>()
    private var found = false

    fun addAllOfMember(flat: Flat) {
      absorb(flat)
      required += flat.required
    }

    fun addAlternatives(flats: List<Flat>) {
      flats.forEach(::absorb)
      required += flats.map { it.required }.reduce { shared, next -> shared intersect next }
    }

    fun addDeclared(declared: Map<String, Merged>) {
      found = true
      properties += declared
    }

    fun build(): Flat? = if (found && properties.isNotEmpty()) Flat(properties, required) else null

    private fun absorb(flat: Flat) {
      found = true
      flat.properties.forEach { (key, value) ->
        properties[key] = properties[key]?.let { merge(it, value) } ?: value
      }
    }
  }

  private fun merge(a: Merged, b: Merged): Merged =
    Merged(
      node = a.node,
      nullable = a.nullable || b.nullable,
      enumValues =
        if (a.enumValues.isNotEmpty() && b.enumValues.isNotEmpty()) a.enumValues + b.enumValues
        else emptySet(),
    )

  private class Flat(val properties: Map<String, Merged>, val required: Set<String>)

  private class Merged(val node: Node, val nullable: Boolean, val enumValues: Set<String>)

  @JvmInline
  private value class Node(val map: Map<*, *>) {
    fun child(key: String): Node? = (map[key] as? Map<*, *>)?.let(::Node)

    fun children(key: String): List<Node> =
      (map[key] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>().map(::Node)

    fun strings(key: String): List<String> =
      (map[key] as? List<*>).orEmpty().filterIsInstance<String>()

    fun flag(key: String): Boolean = map[key] == true

    fun entries(): List<Pair<String, Node>> =
      map.entries.mapNotNull { (key, value) ->
        (value as? Map<*, *>)?.let { key.toString() to Node(it) }
      }

    fun refName(): String? = (map["\$ref"] as? String)?.substringAfterLast('/')
  }

  companion object {
    const val RESOURCE = "fapi/fapi-2026-08-20.yml"

    fun load(): SpecSchemas {
      val stream =
        requireNotNull(SpecSchemas::class.java.classLoader?.getResourceAsStream(RESOURCE)) {
          "Test resource $RESOURCE is missing"
        }
      val document = stream.use { Load(LoadSettings.builder().build()).loadFromInputStream(it) }
      val schemas =
        requireNotNull(Node(document as Map<*, *>).child("components")?.child("schemas")) {
          "$RESOURCE has no components.schemas"
        }
      val version =
        requireNotNull(Regex("""\d{4}-\d{2}-\d{2}""").find(RESOURCE)?.value) {
          "$RESOURCE has no date in its name"
        }
      return SpecSchemas(schemas.entries().toMap(), version)
    }
  }
}
