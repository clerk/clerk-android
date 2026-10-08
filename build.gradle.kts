import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.sortDependencies) apply false
  alias(libs.plugins.jetbrains.kotlin.jvm) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.binaryCompatibilityValidator) apply false
}

val projectLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")

// Shared with config/bin/ktfmt and config/bin/detekt-cli (pre-commit hook) via the version catalog.
val ktfmtVersion = projectLibs.findVersion("ktfmt").get().requiredVersion
val detektVersion = projectLibs.findVersion("detekt").get().requiredVersion

val typeResolvedDetektTasks = setOf("detektDebug")
val typeResolvedDetektBaselineTasks = setOf("detektBaselineDebug")

allprojects {
  apply(plugin = "com.diffplug.spotless")
  configure<SpotlessExtension> {
    format("misc") {
      target("*.md", ".gitignore")
      trimTrailingWhitespace()
      endWithNewline()
    }
    kotlin {
      target("**/*.kt")
      ktfmt(ktfmtVersion).googleStyle()
      trimTrailingWhitespace()
      endWithNewline()
      targetExclude("**/spotless.kt")
    }
    kotlinGradle {
      target("*.kts")
      ktfmt(ktfmtVersion).googleStyle()
      trimTrailingWhitespace()
      endWithNewline()
      targetExclude("**/spotless.gradle")
    }
  }

  apply(plugin = "dev.detekt")
  configure<DetektExtension> {
    toolVersion = detektVersion
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/detekt-baseline.xml")
  }
  tasks.withType<Detekt>().configureEach {
    jvmTarget = projectLibs.findVersion("jvmTarget").get().requiredVersion
  }

  // The plain `detekt` task has no compile classpath, so rules that need type resolution (for
  // example InjectDispatcher) silently skip. CI also runs the type-resolved task for each SDK
  // module's production sources. Those report a different set of findings, so each module keeps
  // its own baseline for them (regenerate with the matching detektBaseline* task).
  tasks
    .withType<Detekt>()
    .matching { it.name in typeResolvedDetektTasks }
    .configureEach { baseline.set(file("detekt-baseline.xml")) }
  tasks
    .withType<DetektCreateBaselineTask>()
    .matching { it.name in typeResolvedDetektBaselineTasks }
    .configureEach { baseline.set(file("detekt-baseline.xml")) }

  val detektProjectBaseline by
    tasks.registering(DetektCreateBaselineTask::class) {
      description = "Overrides current baseline."
      buildUponDefaultConfig.set(true)
      ignoreFailures.set(true)
      parallel.set(true)
      setSource(files(rootDir))
      config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
      baseline.set(file("$rootDir/config/detekt/detekt-baseline.xml"))
      include("**/*.kt")
      include("**/*.kts")
      exclude("**/resources/**")
      exclude("**/build/**")
    }
}

dokka { dokkaPublications.html { outputDirectory.set(rootDir.resolve("docs/")) } }

dependencies {
  dokka(project(":source:api"))
  dokka(project(":source:ui"))
}

tasks.register("verifyPublishedArtifacts") {
  group = "verification"
  description = "Checks consumer rules and published dependency metadata for forbidden entries."
  dependsOn(
    ":source:api:generateMetadataFileForMavenPublication",
    ":source:api:generatePomFileForMavenPublication",
    ":source:ui:generateMetadataFileForMavenPublication",
    ":source:ui:generatePomFileForMavenPublication",
  )

  val apiConsumerRules = file("source/api/consumer-rules.pro")
  val uiConsumerRules = file("source/ui/consumer-rules.pro")
  val apiPublication = project(":source:api").layout.buildDirectory.dir("publications/maven")
  val uiPublication = project(":source:ui").layout.buildDirectory.dir("publications/maven")
  inputs.files(apiConsumerRules, uiConsumerRules)
  inputs.dir(apiPublication)
  inputs.dir(uiPublication)

  doLast {
    fun assertRulesDoNotContain(file: File, forbiddenRules: List<String>) {
      val rules = file.readText()
      forbiddenRules.forEach { rule ->
        check(rule !in rules) { "${file.relativeTo(rootDir)} must not contain '$rule'." }
      }
    }

    fun assertCoordinateIsNotPublished(directory: File, coordinate: String) {
      val (group, artifact) = coordinate.split(":", limit = 2)
      val pomPattern =
        Regex(
          "<groupId>\\s*${Regex.escape(group)}\\s*</groupId>\\s*" +
            "<artifactId>\\s*${Regex.escape(artifact)}\\s*</artifactId>"
        )
      val modulePattern =
        Regex(
          "\\\"group\\\"\\s*:\\s*\\\"${Regex.escape(group)}\\\"\\s*,\\s*" +
            "\\\"module\\\"\\s*:\\s*\\\"${Regex.escape(artifact)}\\\""
        )
      val metadataFiles =
        listOf(directory.resolve("pom-default.xml"), directory.resolve("module.json"))
      metadataFiles.forEach { metadataFile ->
        check(metadataFile.isFile) {
          "Expected publication metadata file ${metadataFile.relativeTo(rootDir)} to exist."
        }
      }
      val publicationMetadata = metadataFiles.joinToString("\n") { it.readText() }
      check(!pomPattern.containsMatchIn(publicationMetadata)) {
        "$coordinate must not be published from ${directory.relativeTo(rootDir)}."
      }
      check(!modulePattern.containsMatchIn(publicationMetadata)) {
        "$coordinate must not be published from ${directory.relativeTo(rootDir)}."
      }
    }

    assertRulesDoNotContain(
      apiConsumerRules,
      listOf(
        "-keep class com.clerk.api.**",
        "-keep class com.clerk.sdk.**",
        "-keep class com.auth0.android.jwt.**",
        "-keep class com.google.android.gms.**",
        "-keep class androidx.credentials.**",
        "-keepclassmembers enum *",
      ),
    )
    assertRulesDoNotContain(
      uiConsumerRules,
      listOf(
        "-keep class com.clerk.ui.**",
        "-keep class androidx.compose.runtime.**",
        "class * extends androidx.lifecycle.ViewModel",
        "-keep class androidx.compose.material3.**",
        "-keep class coil3.**",
      ),
    )

    fun assertPomName(directory: File, expectedName: String) {
      val pom = directory.resolve("pom-default.xml").readText()
      check("<name>$expectedName</name>" in pom) {
        "${directory.relativeTo(rootDir)}/pom-default.xml must be named '$expectedName'."
      }
    }

    assertPomName(apiPublication.get().asFile, "Clerk Android API")
    assertPomName(uiPublication.get().asFile, "Clerk Android UI")

    listOf("com.google.devtools.ksp:symbol-processing-api").forEach {
      assertCoordinateIsNotPublished(apiPublication.get().asFile, it)
    }
    listOf(
        "androidx.compose.ui:ui-tooling",
        "androidx.compose.ui:ui-tooling-preview",
        "androidx.compose.ui:ui-tooling-preview-android",
        "androidx.test:core-ktx",
      )
      .forEach { assertCoordinateIsNotPublished(uiPublication.get().asFile, it) }
  }
}

tasks.named("check") { dependsOn("verifyPublishedArtifacts") }

// `jdk` in the version catalog is the JDK the build and tests run on (Robolectric and Paparazzi
// need 21). `jvmTarget` is the bytecode level we publish, so consumers on Java 17 are unaffected.
val buildJdk = libs.versions.jdk.map(JavaLanguageVersion::of)
val bytecodeTarget = JavaVersion.toVersion(libs.versions.jvmTarget.get())

// Neither KGP's abiValidation nor the BCV plugin registers tasks for Android libraries built
// with AGP 9 built-in Kotlin, so we register BCV's task types against the release compilation.
val abiTrackedAndroidLibraries = setOf(":source:api", ":source:ui", ":source:telemetry")

subprojects {
  if (path !in abiTrackedAndroidLibraries) return@subprojects
  plugins.withId("com.android.library") {
    val abiRuntime =
      configurations.create("abiValidationRuntime") {
        isCanBeConsumed = false
        isCanBeResolved = true
      }
    dependencies {
      add(abiRuntime.name, projectLibs.findLibrary("abi-asm").get())
      add(abiRuntime.name, projectLibs.findLibrary("abi-asmTree").get())
      add(abiRuntime.name, projectLibs.findLibrary("abi-kotlinMetadataJvm").get())
    }

    val referenceDump = layout.projectDirectory.file("api/${project.name}.api")
    val apiBuild =
      tasks.register<KotlinApiBuildTask>("apiBuild") {
        description = "Builds the public ABI dump of the release variant."
        runtimeClasspath.from(abiRuntime)
        outputApiFile.set(layout.buildDirectory.file("api/${project.name}.api"))
        // The Compose compiler emits a public ComposableSingletons$<File>Kt class holding
        // getLambda$<hash>$<module> getters for every lambda it hoists, including those in private
        // previews. They are not callable API and their hashes change with unrelated edits, so
        // drop those classes from the dump. ignoredClasses only takes exact names, hence the
        // post-processing.
        val dumpFile = outputApiFile
        doLast {
          val file = dumpFile.get().asFile
          val blocks = file.readText().split(Regex("\n\n+"))
          val kept = blocks.filterNot { block ->
            block.lineSequence().firstOrNull()?.contains("/ComposableSingletons$") == true
          }
          if (kept.size != blocks.size) file.writeText(kept.joinToString("\n\n"))
        }
      }
    extensions.getByType<KotlinAndroidProjectExtension>().target.compilations.configureEach {
      if (name == "release") {
        val release = this
        apiBuild.configure {
          inputClassesDirs.from(
            release.compileTaskProvider.flatMap { (it as KotlinJvmCompile).destinationDirectory }
          )
        }
      }
    }

    val apiCheck =
      tasks.register<KotlinApiCompareTask>("apiCheck") {
        group = "verification"
        description = "Checks the public ABI against the checked-in api/${project.name}.api dump."
        projectApiFile.set(referenceDump)
        generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
      }
    tasks.register<Copy>("apiDump") {
      description = "Overwrites api/${project.name}.api with the current public ABI."
      from(apiBuild.flatMap { it.outputApiFile })
      into(layout.projectDirectory.dir("api"))
    }
    tasks.named("check") { dependsOn(apiCheck) }
  }
}

subprojects {
  plugins.withType<JavaPlugin> {
    the<JavaPluginExtension>().toolchain.languageVersion.set(buildJdk)
  }

  tasks.withType<Test>().configureEach {
    javaLauncher.set(
      project.extensions.getByType<JavaToolchainService>().launcherFor {
        languageVersion.set(buildJdk)
      }
    )
  }

  plugins.withId("com.android.library") {
    the<com.android.build.api.dsl.LibraryExtension>().compileOptions {
      sourceCompatibility = bytecodeTarget
      targetCompatibility = bytecodeTarget
    }
  }

  plugins.withId("com.android.application") {
    the<com.android.build.api.dsl.ApplicationExtension>().compileOptions {
      sourceCompatibility = bytecodeTarget
      targetCompatibility = bytecodeTarget
    }
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions { jvmTarget.set(libs.versions.jvmTarget.map(JvmTarget::fromTarget)) }
  }
}
