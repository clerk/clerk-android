import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private val workbenchKey = "WORKBENCH_CLERK_PUBLISHABLE_KEY"

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.gms)
  alias(libs.plugins.firebase.appDistribution)
}

android {
  namespace = "com.clerk.workbench"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.clerk.workbench"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.compileSdk.get().toInt()
    versionCode = 2
    versionName = "AuthView-v2"

    val isCI = System.getenv("CI")?.toBoolean() == true
    val clerkPublishableKey = project.findProperty(workbenchKey) as String?

    if (clerkPublishableKey.isNullOrEmpty() && !isCI) {
      throw GradleException("Missing $workbenchKey in gradle.properties")
    }

    val keyValue = clerkPublishableKey ?: "pk_test_placeholder_for_ci"
    buildConfigField("String", workbenchKey, "\"${keyValue}\"")
  }

  buildTypes {
    getByName("debug") {
      firebaseAppDistribution {
        artifactType = "APK"
        artifactPath = "workbench/build/outputs/apk/debug/workbench-debug.apk"
      }
    }
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
  implementation(platform(libs.firebase.bom))
  implementation(libs.activity.compose)
  implementation(libs.androidx.compose.icons)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.material3)
  implementation(projects.source.api)
  implementation(projects.source.ui)
}
