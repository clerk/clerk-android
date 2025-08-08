import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.dokka)
}

android {
  namespace = "com.clerk.ui"
  compileSdk = 36

  defaultConfig {
    minSdk = 24

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

  buildFeatures { compose = true }
}

tasks.withType<DokkaTaskPartial>().configureEach {
  dependencies { dokkaPlugin(libs.versioning.plugin) }
  moduleName.set("Clerk Android UI")
  suppressInheritedMembers.set(true)
  dokkaSourceSets.configureEach { reportUndocumented.set(true) }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.ui)
  implementation(libs.material)
  implementation(libs.material3)

  compileOnly(projects.clerk.source.api)

  testImplementation(libs.junit)

  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)

  lintChecks(libs.compose.lints)
}
