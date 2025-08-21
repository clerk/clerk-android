/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package foundry.gradle.dependencyrake

import com.autonomousapps.AbstractPostProcessingTask
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.FlatCoordinates
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectCoordinates
import java.io.File
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

private const val IGNORE_COMMENT = "// dependency-rake=ignore"

private val PREFERRED_BUNDLE_IDENTIFIERS =
  mapOf("com.google.android.play:core" to "com.google.android.play:core-ktx")

private val MANAGED_DEPENDENCIES =
  setOf(
    "androidx.databinding:viewbinding",
    "org.jetbrains.kotlin:kotlin-stdlib",
    "org.robolectric:shadowapi",
    "org.robolectric:shadows-framework",
  )

@DisableCachingByDefault
abstract class RakeDependencies
@Inject
constructor(objects: ObjectFactory, providers: ProviderFactory) : AbstractPostProcessingTask() {

  @get:Input abstract val identifierMap: MapProperty<String, String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val buildFileProperty: org.gradle.api.file.RegularFileProperty

  init {
    @Suppress("LeakingThis") doNotTrackState("This task modifies build scripts in place.")
  }

  @get:Input
  val modes: SetProperty<AnalysisMode> =
    objects
      .setProperty(AnalysisMode::class.java)
      .convention(
        providers
          .gradleProperty("foundry.dependencyrake.modes")
          .map { it.split(',').map(String::trim).filter(String::isNotEmpty).map(AnalysisMode::valueOf).toSet() }
          .orElse(
            setOf(
              AnalysisMode.COMPILE_ONLY,
              AnalysisMode.UNUSED,
              AnalysisMode.MISUSED,
              AnalysisMode.PLUGINS,
              AnalysisMode.ABI,
            )
          )
      )

  @get:Input
  val dryRun: Property<Boolean> =
    objects
      .property(Boolean::class.java)
      .convention(
        providers.gradleProperty("foundry.dependencyrake.dryRun").map { it.toBoolean() }.orElse(false)
      )

  @get:Input abstract val noApi: Property<Boolean>

  @get:OutputFile abstract val missingIdentifiersFile: org.gradle.api.file.RegularFileProperty

  init {
    group = "rake"
  }

  @TaskAction
  fun rake() {
    if (identifierMap.get().isEmpty()) {
      logger.warn("No identifier map found. Skipping rake.")
      return
    }
    val noApi = noApi.get()
    val projectAdvice = projectAdvice()
    // Plugin advice not applied in this adaptation
    val advices: Set<Advice> = projectAdvice.dependencyAdvice
    val buildFile = buildFileProperty.asFile.get()
    val missingIdentifiers = mutableSetOf<String>()
    logger.lifecycle("🌲 Raking $buildFile ")
    rakeProject(buildFile, advices, null, noApi, missingIdentifiers)
    val identifiersFile = missingIdentifiersFile.asFile.get()
    if (missingIdentifiers.isNotEmpty()) {
      logger.lifecycle("⚠️ Missing identifiers found, written to $identifiersFile")
    }
    identifiersFile.writeText(missingIdentifiers.sorted().joinToString("\n"))
  }

  @Suppress("LongMethod", "ComplexMethod")
  private fun rakeProject(
    buildFile: File,
    advices: Set<Advice>,
    @Suppress("UNUSED_PARAMETER") redundantPlugins: Any? = null,
    noApi: Boolean,
    missingIdentifiers: MutableSet<String>,
  ) {
    val resolvedModes = modes.get()
    val abiModeEnabled = AnalysisMode.ABI in resolvedModes

    val unusedDepsToRemove =
      if (AnalysisMode.UNUSED in resolvedModes) {
        advices
          .filter { it.isRemove() }
          .filterNot { it.coordinates.identifier in MANAGED_DEPENDENCIES }
          .associateBy { it.toDependencyString("UNUSED", missingIdentifiers) }
      } else {
        emptyMap()
      }

    val misusedDepsToRemove =
      if (AnalysisMode.MISUSED in resolvedModes) {
        advices
          .filter { it.isRemove() }
          .filterNot { it.coordinates.identifier in MANAGED_DEPENDENCIES }
          .associateBy { it.toDependencyString("MISUSED", missingIdentifiers) }
      } else {
        emptyMap()
      }

    val depsToRemove = (unusedDepsToRemove + misusedDepsToRemove)

    val depsToChange =
      if (AnalysisMode.ABI in resolvedModes) {
        advices
          .filter { it.isChange() }
          .filterNot { it.coordinates.identifier in MANAGED_DEPENDENCIES }
          .associateBy { it.toDependencyString("CHANGE", missingIdentifiers) }
      } else {
        emptyMap()
      }

    val depsToAdd =
      if (AnalysisMode.MISUSED in resolvedModes) {
        advices
          .filter { it.isAdd() }
          .filterNot { it.coordinates.identifier in MANAGED_DEPENDENCIES }
          .associateBy { it.coordinates.identifier }
          .toMutableMap()
      } else {
        mutableMapOf()
      }

    val compileOnlyDeps =
      if (AnalysisMode.COMPILE_ONLY in resolvedModes) {
        advices
          .filter { it.isCompileOnly() }
          .associateBy { it.toDependencyString("ADD-COMPILE-ONLY", missingIdentifiers) }
      } else {
        emptyMap()
      }

    val newLines = mutableListOf<String>()
    buildFile.useLines { lines ->
      var inDependenciesBlock = false
      var done = false
      var ignoreNext = false
      lines.forEach { line ->
        if (done) {
          newLines += line
          return@forEach
        }
        if (!inDependenciesBlock) {
          if (line.trimStart().startsWith("dependencies {")) {
            inDependenciesBlock = true
          }
          newLines += line
          return@forEach
        } else {
          when {
            line.trimEnd() == "}" -> {
              done = true
              depsToAdd.entries
                .mapNotNull { (_, advice) ->
                  advice.coordinates.toDependencyNotation("ADD-NEW", missingIdentifiers)?.let { newNotation ->
                    var newConfiguration = advice.toConfiguration!!
                    if (noApi && newConfiguration == "api") {
                      newConfiguration = "implementation"
                    }
                    "  $newConfiguration($newNotation)"
                  }
                }
                .sorted()
                .forEach {
                  logger.lifecycle("  ➕ Adding '${it.trimStart()}'")
                  newLines += it
                }

              newLines += line
              return@forEach
            }
            IGNORE_COMMENT in line -> {
              ignoreNext = true
              newLines += line
              return@forEach
            }
            ignoreNext -> {
              ignoreNext = false
              newLines += line
              return@forEach
            }
            depsToRemove.keys.any { it in line } -> {
              if (" {" in line) {
                logger.lifecycle("  🤔 Could not remove '$line'")
                newLines += line
                return@forEach
              }
              logger.lifecycle("  ⛔ Removing '${line.trimStart()}'")

              advices
                .filter { it.isAdd() }
                .mapNotNull { depsToAdd.remove(it.coordinates.identifier) }
                .mapNotNull { advice ->
                  advice.coordinates.toDependencyNotation("ADD", missingIdentifiers)?.let { newNotation ->
                    val newConfiguration =
                      if (!abiModeEnabled) {
                        "implementation"
                      } else {
                        advice.toConfiguration
                      }
                    "  $newConfiguration($newNotation)"
                  }
                }
                .sorted()
                .forEach { newLines += it }
              return@forEach
            }
            depsToChange.keys.any { it in line } || compileOnlyDeps.keys.any { it in line } -> {
              if (" {" in line) {
                logger.lifecycle("  🤔 Could not modify '$line'")
                newLines += line
                return@forEach
              }
              val which =
                if (depsToChange.keys.any { it in line }) depsToChange else compileOnlyDeps
              val (_, abiDep) =
                which.entries.first { (_, v) ->
                  v.coordinates.toDependencyNotation("ABI", missingIdentifiers)?.let { it in line }
                    ?: false
                }
              val oldConfiguration = abiDep.fromConfiguration!!
              var newConfiguration = abiDep.toConfiguration!!
              if (noApi && newConfiguration == "api") {
                newConfiguration = "implementation"
              }
              val newLine = line.replace("$oldConfiguration(", "$newConfiguration(")
              logger.lifecycle("  ✏️ Modifying configuration")
              logger.lifecycle("     -${line.trimStart()}")
              logger.lifecycle("     +${newLine.trimStart()}")
              newLines += newLine
              return@forEach
            }
            else -> {
              newLines += line
              return@forEach
            }
          }
        }
      }
    }

    // Skipping plugin advice removal in this adaptation

    val fileToWrite =
      if (!dryRun.get()) {
        buildFile
      } else {
        buildFile.parentFile.resolve("new-build.gradle.kts").apply {
          if (exists()) delete()
          createNewFile()
        }
      }

    fileToWrite.writeText(newLines.cleanLineFormatting().joinToString("\n"))
  }

  private fun Coordinates.mapIdentifier(
    context: String,
    missingIdentifiers: MutableSet<String>,
  ): Coordinates? {
    return when (this) {
      is ModuleCoordinates -> {
        val preferredIdentifier = PREFERRED_BUNDLE_IDENTIFIERS.getOrDefault(identifier, identifier)
        val newIdentifier =
          identifierMap.get()[preferredIdentifier]
            ?: run {
              logger.lifecycle("($context) Unknown identifier: $identifier")
              missingIdentifiers += identifier
              return null
            }
        ModuleCoordinates(newIdentifier, resolvedVersion, gradleVariantIdentification)
      }
      is FlatCoordinates,
      is IncludedBuildCoordinates,
      is ProjectCoordinates -> this
    }
  }

  private fun Advice.toDependencyString(
    context: String,
    missingIdentifiers: MutableSet<String>,
  ): String {
    val fromConfig = fromConfiguration ?: error("Transitive dep $this")
    val notation = coordinates.toDependencyNotation(context, missingIdentifiers)
    return "$fromConfig($notation)"
  }

  private fun Coordinates.toDependencyNotation(
    context: String,
    missingIdentifiers: MutableSet<String>,
  ): String? {
    return when (this) {
      is ProjectCoordinates -> "projects.${convertProjectPathToAccessor(identifier)}"
      is ModuleCoordinates -> mapIdentifier(context, missingIdentifiers)?.identifier
      is FlatCoordinates -> gav()
      is IncludedBuildCoordinates -> gav()
    }
  }

  enum class AnalysisMode {
    UNUSED,
    COMPILE_ONLY,
    ABI,
    MISUSED,
    PLUGINS,
  }
}

private fun List<String>.cleanLineFormatting(): List<String> {
  val cleanedBlankLines = mutableListOf<String>()
  var blankLineCount = 0
  for (newLine in this) {
    if (newLine.isBlank()) {
      if (blankLineCount == 1) {
      } else {
        blankLineCount++
        cleanedBlankLines += newLine
      }
    } else {
      blankLineCount = 0
      cleanedBlankLines += newLine
    }
  }
  return cleanedBlankLines.padNewline()
}

private fun List<String>.padNewline(): List<String> {
  val noEmpties = dropLastWhile { it.isBlank() }
  return noEmpties + ""
}

private fun convertProjectPathToAccessor(path: String): String {
  return path.trimStart(':').split(':').joinToString(".")
}

