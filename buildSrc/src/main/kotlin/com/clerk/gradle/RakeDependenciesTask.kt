package com.clerk.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault

/**
 * Removes the dependency declarations that the Dependency Analysis plugin reports as unused.
 *
 * A removed declaration can be the only path to a transitive dependency the code compiles against,
 * so the rake also declares each used dependency that a removal would drop from a compile
 * classpath. When such a dependency has no version catalog entry, the removals that provide it are
 * left in place and reported instead, so a rake never leaves the build unable to compile.
 */
@DisableCachingByDefault(because = "Rewrites the project's build script in place")
abstract class RakeDependenciesTask : DefaultTask() {
  /** The Dependency Analysis plugin's final advice for this project. */
  @get:InputFile @get:PathSensitive(PathSensitivity.NONE) abstract val advice: RegularFileProperty

  /** The plugin's per-source-set compile classpath graphs for this project. */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val dependencyGraphs: ConfigurableFileCollection

  /** Maps each `group:name` in the version catalog to its `libs.…` accessors. */
  @get:Input abstract val catalogAccessors: MapProperty<String, List<String>>

  /** Maps each compile classpath to the declarable configurations it draws from. */
  @get:Input abstract val classpathConfigurations: MapProperty<String, Set<String>>

  /** Maps each declarable configuration to the dependencies it declares. */
  @get:Input abstract val declarations: MapProperty<String, Set<String>>

  @get:Input abstract val projectPath: Property<String>

  @get:Internal abstract val buildFile: RegularFileProperty

  @get:Input
  @get:Option(option = "preview", description = "Prints the changes without writing them.")
  abstract val preview: Property<Boolean>

  init {
    preview.convention(false)
  }

  @TaskAction
  fun rake() {
    val notations = Notations(catalogAccessors.get())
    val plan =
      DependencyRake(
          advice = readAdvice(advice.get().asFile),
          graphs = dependencyGraphs.files.map { readGraph(it, projectPath.get()) },
          classpathConfigurations = classpathConfigurations.get(),
          declarations = declarations.get(),
          notations = notations,
        )
        .plan()

    val file = buildFile.get().asFile
    val edit = BuildScriptEdit(file.readLines())
    val kept = plan.kept.toMutableMap()
    val removed =
      plan.removals.filter { removal ->
        edit.remove(removal.from!!, notations.patterns(removal.coordinate)).also { found ->
          if (!found) kept[removal] = "no single-line declaration of it was found"
        }
      }
    val added =
      plan.additions.mapNotNull { addition ->
        val notation = notations.declare(addition.coordinate)
        if (notation == null) {
          logger.warn("${projectPath.get()} compiles against ${addition.id}; declare it by hand")
        }
        notation?.let { "${addition.to}($it)" }?.also(edit::add)
      }

    report(removed, added, kept)
    if (!preview.get() && (removed.isNotEmpty() || added.isNotEmpty())) {
      file.writeText(edit.text())
    }
  }

  private fun report(removed: List<Advice>, added: List<String>, kept: Map<Advice, String>) {
    if (removed.isEmpty() && added.isEmpty() && kept.isEmpty()) return
    logger.lifecycle("${if (preview.get()) "Would rake" else "Raked"} ${projectPath.get()}:")
    removed.forEach { logger.lifecycle("  - ${it.from}(${it.id})") }
    added.forEach { logger.lifecycle("  + $it") }
    kept.forEach { (removal, reason) ->
      logger.lifecycle("  kept ${removal.from}(${removal.id}): $reason")
    }
  }
}
