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

subprojects {
  plugins.withType<JavaPlugin> {
    the<JavaPluginExtension>().toolchain {
      languageVersion.set(libs.versions.jdk.map(JavaLanguageVersion::of))
    }
  }

  plugins.withId("com.android.library") {
    the<com.android.build.gradle.BaseExtension>().compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
    }
  }

  // Kotlin configuration
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions { jvmTarget.set(libs.versions.jvmTarget.map(JvmTarget::fromTarget)) }
  }
}
