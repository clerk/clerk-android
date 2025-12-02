import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private val linearCloneKey = "LINEAR_CLONE_CLERK_PUBLISHABLE_KEY"

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.plugin.serialization)
}

android {
  namespace = "com.clerk.linearclone"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.clerk.linearclone"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.compileSdk.get().toInt()

    val isCI = System.getenv("CI")?.toBoolean() == true
    val clerkPublishableKey = project.findProperty(linearCloneKey) as String?

    if (clerkPublishableKey.isNullOrEmpty() && !isCI) {
      throw GradleException("Missing $linearCloneKey in gradle.properties")
    }

    val keyValue = clerkPublishableKey ?: "pk_test_placeholder_for_ci"
    buildConfigField("String", linearCloneKey, "\"${keyValue}\"")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.activity.compose)
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.coil)
  implementation(libs.coil.okhttp)
  implementation(libs.core.ktx)
  implementation(libs.kotlinx.serialization)
  implementation(libs.material3)
  implementation(libs.navigation.compose)
  implementation(projects.source.api)

  lintChecks(libs.compose.lints)
}

tasks.matching { it.name.startsWith("dokka") }.configureEach { enabled = false }
