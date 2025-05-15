plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.ksp)
  id("maven-publish")
}

android {
  namespace = "com.clerk.sdk"
  compileSdk = 35

  defaultConfig { minSdk = 24 }

  buildTypes { release { isMinifyEnabled = false } }

  publishing {
    singleVariant("release") {
      withSourcesJar()
      withJavadocJar()
    }
  }
}

afterEvaluate {
  publishing {
    publications {
      register<MavenPublication>("release") {
        groupId = "com.clerk"
        artifactId = "clerk-android"
        version = "0.1.0"

        afterEvaluate { from(components["release"]) }
      }
    }
  }
}

dependencies {
  implementation(libs.androidx.datastore)
  implementation(libs.androidx.lifecycle)
  implementation(libs.androidx.lifecycle.process)
  api(libs.eithernet)
  implementation(libs.eithernet.retrofit)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.serialization)
  implementation(libs.ksp.api)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging)
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx)
  implementation(projects.mapGenerator)

  testImplementation(libs.androidx.arch.test)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockito)
  testImplementation(libs.mockk)
  testImplementation(libs.robolectric)

  ksp(projects.mapGenerator)
}
