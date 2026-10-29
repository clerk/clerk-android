package com.clerk.gradle

import groovy.json.JsonSlurper
import java.io.File

internal data class Coordinate(val type: String, val identifier: String)

/** One piece of the Dependency Analysis plugin's advice: a removal, an addition, or a move. */
internal data class Advice(val coordinate: Coordinate, val from: String?, val to: String?) {
  val id: String
    get() = coordinate.identifier

  val isRemoval: Boolean
    get() = from != null && to == null

  val isAddition: Boolean
    get() = from == null && to != null
}

/** A compile classpath's dependency graph, keyed by `group:name` or project path. */
internal class Graph(val classpath: String, val root: String, val edges: Map<String, Set<String>>) {
  fun reachableFrom(start: String, cutFromRoot: Set<String> = emptySet()): Set<String> {
    val seen = mutableSetOf(start)
    val queue = ArrayDeque(listOf(start))
    while (queue.isNotEmpty()) {
      val node = queue.removeFirst()
      edges[node].orEmpty().forEach { next ->
        if (!(node == root && next in cutFromRoot) && seen.add(next)) queue += next
      }
    }
    return seen
  }
}

/** How a dependency is written in this repo's build scripts. */
internal class Notations(
  private val catalogAccessors: Map<String, List<String>>,
  rootProjectName: String = "",
) {
  private val rootAccessor = accessorSegment(rootProjectName).replaceFirstChar(Char::lowercaseChar)

  /** The notation to declare [coordinate] with, or null when it has no version catalog entry. */
  fun declare(coordinate: Coordinate): String? =
    when (coordinate.type) {
      "project" -> "projects." + projectAccessor(coordinate.identifier)
      else -> catalogAccessors[coordinate.identifier]?.first()
    }

  /**
   * Regex alternatives matching every way [coordinate] may already be declared, including project
   * accessors that start with the root project's name, such as `projects.clerk.source.api`.
   */
  fun patterns(coordinate: Coordinate): List<String> =
    when (coordinate.type) {
      "project" -> {
        val root = if (rootAccessor.isEmpty()) "" else """(\.${Regex.escape(rootAccessor)})?"""
        listOf(
          """projects$root\.${Regex.escape(projectAccessor(coordinate.identifier))}""",
          """project\(\s*"${Regex.escape(coordinate.identifier)}"\s*\)""",
        )
      }
      else ->
        catalogAccessors[coordinate.identifier].orEmpty().map(Regex::escape) +
          """"${Regex.escape(coordinate.identifier)}(:[^"]*)?""""
    }

  private fun projectAccessor(path: String): String =
    path.split(':').filter(String::isNotEmpty).joinToString(".", transform = ::accessorSegment)

  private fun accessorSegment(name: String): String =
    name
      .split('-', '_')
      .mapIndexed { i, part -> if (i == 0) part else part.replaceFirstChar(Char::uppercaseChar) }
      .joinToString("")
}

internal class RakePlan(
  val removals: List<Advice>,
  val additions: List<Advice>,
  val kept: Map<Advice, String>,
)

/**
 * Decides which unused declarations a project can drop, and which used transitive dependencies it
 * must then declare itself.
 */
internal class DependencyRake(
  private val advice: List<Advice>,
  private val graphs: List<Graph>,
  private val classpathConfigurations: Map<String, Set<String>>,
  private val declarations: Map<String, Set<String>>,
  private val notations: Notations,
) {
  /** Plans the rake, leaving out the [unremovable] removals, each kept for the reason given. */
  fun plan(unremovable: Map<Advice, String> = emptyMap()): RakePlan {
    val removals = advice.filter(Advice::isRemoval)
    val kept = unremovable.toMutableMap()
    // The plugin treats main's compileOnly dependencies as visible to tests, but neither AGP nor
    // the Gradle Java plugin puts compileOnly on a test compile classpath.
    removals
      .filter { it.from!!.isTestConfiguration() && it.id in mainCompileOnlyDependencies() }
      .forEach { kept[it] = "main declares it compileOnly, which tests can't see" }

    val raked = (removals - kept.keys).toMutableList()
    while (true) {
      val undeclarable =
        additionsNeededFor(raked).filter { notations.declare(it.coordinate) == null }
      val blocked = raked.associateWith { removal ->
        undeclarable.firstOrNull { missing -> provides(removal, missing) }
      }
      val blockers = blocked.filterValues { it != null }
      if (blockers.isEmpty()) break
      blockers.forEach { (removal, missing) ->
        kept[removal] = "it provides ${missing!!.id}, which has no version catalog entry"
      }
      raked -= blockers.keys
    }
    return RakePlan(raked, additionsNeededFor(raked), kept)
  }

  private fun additionsNeededFor(removals: List<Advice>): List<Advice> {
    val orphans = graphs.associateWith { orphans(it, removals) }
    return advice.filter(Advice::isAddition).filter { addition ->
      graphs.any { graph ->
        addition.to in configurationsOf(graph) && addition.id in orphans.getValue(graph)
      }
    }
  }

  private fun provides(removal: Advice, dependency: Advice): Boolean = graphs.any {
    dependency.id in it.reachableFrom(removal.id)
  }

  /** The dependencies that leave [graph] when the [removals] are applied. */
  private fun orphans(graph: Graph, removals: List<Advice>): Set<String> {
    val configurations = configurationsOf(graph)
    val removed = removals.map { it.from to it.id }.toSet()
    val cut =
      graph.edges[graph.root].orEmpty().filterTo(mutableSetOf()) { id ->
        val declaredIn = declarations.filter { (configuration, ids) ->
          configuration in configurations && id in ids
        }
        declaredIn.isNotEmpty() && declaredIn.keys.all { (it to id) in removed }
      }
    return graph.reachableFrom(graph.root) - graph.reachableFrom(graph.root, cut)
  }

  private fun configurationsOf(graph: Graph): Set<String> =
    classpathConfigurations[graph.classpath].orEmpty()

  private fun mainCompileOnlyDependencies(): Set<String> =
    declarations
      .filterKeys {
        !it.isTestConfiguration() && (it == "compileOnly" || it.endsWith("CompileOnly"))
      }
      .values
      .flatten()
      .toSet()

  private fun String.isTestConfiguration(): Boolean =
    startsWith("test") || startsWith("androidTest")
}

internal fun readAdvice(file: File): List<Advice> {
  val json = JsonSlurper().parse(file) as Map<*, *>
  return (json["dependencyAdvice"] as List<*>).map { entry ->
    entry as Map<*, *>
    val coordinates = entry["coordinates"] as Map<*, *>
    Advice(
      Coordinate(coordinates["type"] as String, coordinates["identifier"] as String),
      entry["fromConfiguration"] as String?,
      entry["toConfiguration"] as String?,
    )
  }
}

internal fun readGraph(file: File, projectPath: String): Graph {
  val json = JsonSlurper().parse(file) as Map<*, *>
  val sourceKind = json["sourceKind"] as Map<*, *>
  val graph = json["graphJson"] as Map<*, *>
  val edges = mutableMapOf<String, MutableSet<String>>()
  (graph["edges"] as List<*>).forEach { edge ->
    edge as Map<*, *>
    edges.getOrPut(nodeId(edge["source"])) { mutableSetOf() } += nodeId(edge["target"])
  }
  return Graph(sourceKind["compileClasspathName"] as String, projectPath, edges)
}

private fun nodeId(node: Any?): String {
  node as Map<*, *>
  val project = node["resolvedProject"] as Map<*, *>?
  return (project?.get("identifier") ?: node["identifier"]) as String
}
