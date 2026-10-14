import org.gradle.api.tasks.testing.Test

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotlin.plugin.serialization)
  id("com.vanniktech.maven.publish")
  alias(libs.plugins.paparazzi)
}

android {
  namespace = "com.clerk.ui"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    consumerProguardFiles("consumer-rules.pro")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    debug { isMinifyEnabled = false }
    release { isMinifyEnabled = false }
  }
  kotlin { jvmToolchain(libs.versions.jdk.get().toInt()) }

  buildFeatures { compose = true }

  testOptions { unitTests.isIncludeAndroidResources = true }

  lint { baseline = file("lint-baseline.xml") }
}

kotlin { explicitApi() }

tasks.withType<Test>().configureEach {
  // Robolectric accesses FileDescriptor internals when initializing Android shared memory.
  jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
  forkEvery = 1
  reports.html.required.set(false)
}

// Compose mapping collection can emit hundreds of parser/tokenizer warnings
// for valid composable signatures on recent Kotlin/Compose toolchains.
// Disable this non-critical reporting task when AGP creates it.
tasks
  .matching { it.name.matches(Regex("report.+ComposeMappingErrors")) }
  .configureEach { enabled = false }

mavenPublishing {
  coordinates("com.clerk", "clerk-android-ui", property("CLERK_UI_VERSION") as String)
  publishToMavenCentral()
  if (providers.gradleProperty("RELEASE_SIGNING_ENABLED").orNull != "false") {
    signAllPublications()
  }
  pom {
    name.set("Clerk Android UI")
    description.set("UI components for Clerk Android SDK")
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

dokka {
  moduleName.set("Clerk Android UI")
  dokkaSourceSets.configureEach { reportUndocumented.set(true) }
  dokkaPublications.configureEach { suppressInheritedMembers.set(true) }
}

dependencies {
  api(projects.clerk.source.api)
  api(projects.source.telemetry)

  implementation(platform(libs.compose.bom))
  implementation(libs.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.icons)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.lifecycle)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.ui)
  implementation(libs.coil)
  implementation(libs.coil.okhttp)
  implementation(libs.google.libphonenumber)
  implementation(libs.kotlinx.immutable)
  implementation(libs.material3)
  implementation(libs.materialKolor)

  debugImplementation(libs.androidx.ui.test.manifest)
  debugImplementation(libs.androidx.ui.tooling)

  compileOnly(libs.androidx.ui.tooling.preview.android)

  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.ui.test.junit4)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockk)
  testImplementation(libs.robolectric)
  testImplementation(libs.turbine)
  testImplementation(projects.clerk.source.api)

  testRuntimeOnly(libs.paparazzi)

  dokkaPlugin(libs.versioning.plugin)

  lintChecks(libs.compose.lints)
}
