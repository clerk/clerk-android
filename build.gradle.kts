import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.android.kotlin.multiplatform.library) apply false
  alias(libs.plugins.android.lint) apply false
}

val projectLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")

allprojects {
  apply(plugin = "com.diffplug.spotless")
  configure<SpotlessExtension> {
    ratchetFrom("origin/main")
    format("misc") {
      target("*.md", ".gitignore")
      trimTrailingWhitespace()
      endWithNewline()
    }
    kotlin {
      target("**/*.kt")
      ktfmt().googleStyle()
      trimTrailingWhitespace()
      endWithNewline()
      targetExclude("**/spotless.kt")
    }
    kotlinGradle {
      target("*.kts")
      ktfmt().googleStyle()
      trimTrailingWhitespace()
      endWithNewline()
      targetExclude("**/spotless.gradle")
    }
  }

  apply(plugin = "io.gitlab.arturbosch.detekt")
  configure<DetektExtension> {
    toolVersion = "1.23.8"
    allRules = true
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/detekt-baseline.xml")
  }
  tasks.withType<Detekt>().configureEach {
    jvmTarget = projectLibs.findVersion("jvmTarget").get().requiredVersion
  }

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

// Root multi-module Dokka output
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
  val apiPublication = layout.projectDirectory.dir("source/api/build/publications/maven")
  val uiPublication = layout.projectDirectory.dir("source/ui/build/publications/maven")
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
      val publicationMetadata =
        directory.walkTopDown().filter(File::isFile).joinToString("\n") { it.readText() }
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

    listOf("com.google.devtools.ksp:symbol-processing-api").forEach {
      assertCoordinateIsNotPublished(apiPublication.asFile, it)
    }
    listOf(
        "androidx.compose.ui:ui-tooling",
        "androidx.compose.ui:ui-tooling-preview",
        "androidx.compose.ui:ui-tooling-preview-android",
        "androidx.test:core-ktx",
      )
      .forEach { assertCoordinateIsNotPublished(uiPublication.asFile, it) }
  }
}

tasks.named("check") { dependsOn("verifyPublishedArtifacts") }

subprojects {
  plugins.withType<JavaPlugin> {
    the<JavaPluginExtension>().toolchain {
      languageVersion.set(libs.versions.jdk.map(JavaLanguageVersion::of))
    }
  }

  plugins.withId("com.android.library") {
    the<com.android.build.api.dsl.LibraryExtension>().compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
    }
  }

  plugins.withId("com.android.application") {
    the<com.android.build.api.dsl.ApplicationExtension>().compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
    }
  }

  // Kotlin configuration
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions { jvmTarget.set(libs.versions.jvmTarget.map(JvmTarget::fromTarget)) }
  }
}
