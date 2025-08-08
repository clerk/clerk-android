import com.diffplug.gradle.spotless.SpotlessExtension
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
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotlin.compose) apply false
}

val projectLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")

allprojects {
  apply(plugin = "com.vanniktech.maven.publish")
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

// Configure Dokka to only generate documentation for the :source:api module
configure(listOf(project(":source:api"))) {
  apply(plugin = "org.jetbrains.dokka")
}

// Create a task to generate documentation only for the API module
tasks.register("dokkaHtmlApi") {
  group = "documentation"
  description = "Generate HTML documentation for the API module only"
  dependsOn(":source:api:dokkaGeneratePublicationHtml")
  doLast {
    copy {
      from(project(":source:api").layout.buildDirectory.dir("dokka/html"))
      into(rootDir.resolve("docs/"))
    }
  }
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

  val sdkVersion = projectLibs.findVersion("clerk-sdk").get().toString()

  mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("com.clerk", "clerk-android", sdkVersion)

    pom {
      name.set("Clerk Android SDK")
      description.set("Clerk SDK for Android")
      inceptionYear.set("2025")
      url.set("https://github.com/clerk/clerk-android")
      licenses {
        license {
          name.set("MIT License")
          url.set("https://github.com/clerk/clerk-android/blob/main/LICENSE")
          distribution.set("https://github.com/clerk/clerk-android/blob/main/LICENSE")
        }
      }

      developers {
        developer {
          id.set("clerk")
          name.set("Clerk")
          url.set("https://clerk.com")
        }
      }
      scm {
        url.set("https://github.com/clerk/clerk-android")
        connection.set("scm:git:git://github.com/clerk/clerk-android.git")
        developerConnection.set("scm:git:ssh://github.com:clerk/clerk-android.git")
      }
    }
  }
}
