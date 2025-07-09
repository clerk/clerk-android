import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

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
}

val projectLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")

allprojects {
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "com.vanniktech.maven.publish")
  apply(plugin = "com.diffplug.spotless")
  apply(plugin = "jacoco")
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

  configure<JacocoPluginExtension> {
    toolVersion = "0.8.11"
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

tasks.dokkaHtmlMultiModule { outputDirectory.set(rootDir.resolve("docs/")) }

// JaCoCo multi-module report
tasks.register<JacocoReport>("jacocoRootReport") {
  group = "verification"
  description = "Generate Jacoco coverage reports for all modules."
  
  // Depend on both test tasks and individual module jacocoTestReport tasks
  dependsOn(subprojects.map { it.tasks.withType<Test>() })
  dependsOn(subprojects.map { it.tasks.named("jacocoTestReport") })
  
  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(false)
  }
  
  val sourceDirs = subprojects.map { it.file("src/main/java") } + subprojects.map { it.file("src/main/kotlin") }
  
  sourceDirectories.setFrom(sourceDirs)
  
  // Class directories - handle both Kotlin and Java classes
  val classDirectories = subprojects.flatMap { subproject ->
    listOf(
      subproject.file("build/tmp/kotlin-classes/debug"),
      subproject.file("build/intermediates/javac/debug/classes")
    ).filter { it.exists() }
  }
  classDirectories.setFrom(classDirectories)
  
  // Execution data - try multiple possible locations for each subproject
  val executionDataFiles = subprojects.flatMap { subproject ->
    listOf(
      subproject.file("build/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"),
      subproject.file("build/jacoco/testDebugUnitTest.exec"),
      subproject.file("build/outputs/code_coverage/debugUnitTest/testDebugUnitTest.exec")
    ).filter { it.exists() }
  }
  executionData.setFrom(executionDataFiles)
  
  doFirst {
    println("Root JaCoCo Task Configuration:")
    println("Source directories: ${sourceDirectories.files}")
    println("Class directories: ${this.classDirectories.files}")
    println("Execution data files: ${executionData.files}")
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
