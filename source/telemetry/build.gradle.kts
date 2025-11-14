plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.android.lint)
  alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
  androidLibrary {
    namespace = "com.clerk.telemetry"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = libs.versions.minSdk.get().toInt()

    withHostTestBuilder {}

    withDeviceTestBuilder { sourceSetTreeName = "test" }
      .configure { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.kotlinx.coroutines)
        implementation(libs.kotlinx.serialization)
      }
    }

    commonTest { dependencies { implementation(libs.kotlin.test) } }

    androidMain {
      dependencies {
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.negototiation)
        implementation(libs.ktor.client.okhttp)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.okhttp)
      }
    }

    getByName("androidDeviceTest") {
      dependencies {
        implementation(libs.androidx.core)
        implementation(libs.androidx.junit)
        implementation(libs.androidx.runner)
      }
    }
  }
}
