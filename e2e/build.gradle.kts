import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private val e2eKey = "E2E_CLERK_PUBLISHABLE_KEY"

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.clerk.e2e"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.clerk.e2e"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.compileSdk.get().toInt()

    val clerkPublishableKey = project.findProperty(e2eKey) as String?
    val keyValue = clerkPublishableKey ?: "pk_test_placeholder_for_e2e"
    buildConfigField("String", e2eKey, "\"${keyValue}\"")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.activity.compose)
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.lifecycle.viewmodel)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.core.ktx)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.material3)
  implementation(projects.source.api)
  implementation(projects.source.ui)

  debugImplementation(libs.androidx.ui.tooling)
}

tasks.matching { it.name.startsWith("dokka") }.configureEach { enabled = false }
