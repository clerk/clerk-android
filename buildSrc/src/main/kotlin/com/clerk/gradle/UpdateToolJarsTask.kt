package com.clerk.gradle

import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Downloads the fat jars that the `config/bin` wrappers run, checks each against the digest Maven
 * Central publishes for it, pins its SHA-256 in `config/bin/tool-checksums.sha256`, and puts it in
 * the cache the wrappers read, so the pre-commit hook doesn't download it again.
 *
 * With [bumpCatalog] set, the resolved versions are also written to `gradle/libs.versions.toml`.
 * Those catalog entries also set the Spotless ktfmt, detekt plugin and sort-dependencies plugin
 * versions, so the Gradle checks and the hook stay on the same versions.
 */
@DisableCachingByDefault(because = "Writes to the source tree and the user's tool cache")
abstract class UpdateToolJarsTask : DefaultTask() {
  @get:Internal abstract val artifacts: SetProperty<ResolvedArtifactResult>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  abstract val jars: ConfigurableFileCollection

  /** Maps each tool's `group:name` to its `[versions]` key in the version catalog. */
  @get:Input abstract val versionKeys: MapProperty<String, String>

  @get:Input abstract val bumpCatalog: Property<Boolean>

  @get:Input abstract val repositoryUrl: Property<String>

  @get:Internal abstract val catalogFile: RegularFileProperty

  @get:Internal abstract val checksumsFile: RegularFileProperty

  @get:Internal abstract val cacheDirectory: DirectoryProperty

  @TaskAction
  fun update() {
    val tools =
      artifacts.get().map { artifact ->
        val id = artifact.id.componentIdentifier as ModuleComponentIdentifier
        Tool(id.moduleIdentifier.toString(), id.version, artifact.file)
      }
    val missing = versionKeys.get().keys - tools.map { it.module }.toSet()
    check(missing.isEmpty()) { "No jar was resolved for $missing" }

    tools.forEach(::verifyPublishedDigest)
    writeChecksums(tools)
    tools.forEach(::copyToCache)
    if (bumpCatalog.get()) bumpVersions(tools)
  }

  private fun verifyPublishedDigest(tool: Tool) {
    val url =
      "${repositoryUrl.get().trimEnd('/')}/${tool.module.replace('.', '/').replace(':', '/')}/" +
        "${tool.version}/${tool.jar.name}"
    val (algorithm, published) =
      PUBLISHED_DIGESTS.firstNotNullOfOrNull { (extension, algorithm) ->
        try {
          val connection =
            URI("$url.$extension").toURL().openConnection().apply {
              connectTimeout = 15_000
              readTimeout = 30_000
            }
          val body = connection.getInputStream().bufferedReader().use { it.readText() }
          algorithm to body.trim().substringBefore(' ')
        } catch (_: FileNotFoundException) {
          null
        }
      } ?: error("Maven Central publishes no SHA-512, SHA-256 or SHA-1 for $url")
    val actual = tool.jar.digest(algorithm)
    check(actual.equals(published, ignoreCase = true)) {
      "${tool.jar.name} doesn't match the $algorithm published at $url"
    }
  }

  private fun writeChecksums(tools: List<Tool>) {
    val file = checksumsFile.get().asFile
    val header = file.readLines().takeWhile { it.startsWith("#") }
    val entries =
      tools
        .sortedBy { versionKeys.get().keys.indexOf(it.module) }
        .map {
          "${it.jar.digest("SHA-256")}  ${it.jar.name}"
        }
    file.writeText((header + entries).joinToString("\n", postfix = "\n"))
  }

  private fun copyToCache(tool: Tool) {
    val cached = cacheDirectory.get().file(tool.jar.name).asFile
    if (!cached.isFile || cached.digest("SHA-256") != tool.jar.digest("SHA-256")) {
      tool.jar.copyTo(cached, overwrite = true)
    }
    logger.lifecycle("${tool.module} ${tool.version} -> $cached")
  }

  private fun bumpVersions(tools: List<Tool>) {
    val file = catalogFile.get().asFile
    var catalog = file.readText()
    tools.forEach { tool ->
      val key = versionKeys.get().getValue(tool.module)
      val entry = Regex("""(?m)^(\s*${Regex.escape(key)}\s*=\s*")([^"]*)(")""")
      val current = checkNotNull(entry.find(catalog)) { "No '$key' version in $file" }
      if (current.groupValues[2] != tool.version) {
        logger.lifecycle("$key: ${current.groupValues[2]} -> ${tool.version}")
        catalog = entry.replaceFirst(catalog, "$1${tool.version}$3")
      }
    }
    file.writeText(catalog)
  }

  private fun File.digest(algorithm: String): String =
    MessageDigest.getInstance(algorithm).digest(readBytes()).joinToString("") { "%02x".format(it) }

  private data class Tool(val module: String, val version: String, val jar: File)

  private companion object {
    val PUBLISHED_DIGESTS = listOf("sha512" to "SHA-512", "sha256" to "SHA-256", "sha1" to "SHA-1")
  }
}

/** A fat jar on Maven Central that a `config/bin` wrapper runs. */
data class ToolJar(val module: String, val classifier: String, val versionKey: String)
