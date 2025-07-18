import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  kotlin("android")
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.plugin.serialization)
}

android {
  namespace = "com.clerk.quickstart"

  defaultConfig {
    applicationId = "com.clerk.quickstart"
    minSdk = 28
    targetSdk = 36
    compileSdk = 36
    versionCode = 1
    versionName = "1.0"

    val isCI = System.getenv("CI")?.toBoolean() == true
    val clerkPublishableKey = project.findProperty("QUICKSTART_CLERK_PUBLISHABLE_KEY") as String?

    if (clerkPublishableKey.isNullOrEmpty() && !isCI) {
      throw GradleException("Missing CLERK_PUBLISHABLE_KEY in gradle.properties")
    }

    val keyValue = clerkPublishableKey ?: "pk_test_placeholder_for_ci"
    buildConfigField("String", "QUICKSTART_CLERK_PUBLISHABLE_KEY", "\"${keyValue}\"")
  }

  kotlin {
    target { compilerOptions { jvmTarget.value(JvmTarget.JVM_24) } }

    compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
      compose = true
      buildConfig = true
    }
  }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.activity.compose)
  implementation(libs.androidx.lifecycle.viewmodel)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.material3)
  implementation(projects.clerk.source)
}
