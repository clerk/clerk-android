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
  compileSdk = 36

  defaultConfig {
    applicationId = "com.clerk.linearclone"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

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
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.coil)
  implementation(libs.coil.okhttp)
  implementation(libs.kotlinx.serialization)
  implementation(libs.material3)
  implementation(libs.navigation.compose)
  implementation(projects.source.api)
  lintChecks(libs.compose.lints)
}

tasks.matching { it.name.startsWith("dokka") }.configureEach { enabled = false }
