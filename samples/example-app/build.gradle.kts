import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  kotlin("android")
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.plugin.serialization)
}

android {
  namespace = "com.clerk.exampleapp"

  defaultConfig {
    applicationId = "com.clerk.exampleapp"
    minSdk = 28
    targetSdk = 36
    compileSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  kotlin {
    target { compilerOptions { jvmTarget.value(JvmTarget.JVM_24) } }

    compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
    hilt {
      // stackoverflow.com/questions/78760124/issue-with-hilt-application-class-gradle-dependency-conflict
      enableAggregatingTask = false
    }
  }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.activity.compose)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.coil)
  implementation(libs.coil.okhttp)
  implementation(libs.hilt.android)
  implementation(libs.material3)
  implementation(libs.navigation.compose)
  implementation(projects.clerk.source)

  ksp(libs.hilt.compiler)

  lintChecks(libs.compose.lints)
}
