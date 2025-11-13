plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.android.lint)
}

kotlin {

  // Target declarations - add or remove as needed below. These define
  // which platforms this KMP module supports.
  // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
  androidLibrary {
    namespace = "com.clerk.telemetry"
    compileSdk = 36
    minSdk = 26

    withHostTestBuilder {}

    withDeviceTestBuilder { sourceSetTreeName = "test" }
      .configure { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
  }

  // For iOS targets, this is also where you should
  // configure native binary output. For more information, see:
  // https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

  // A step-by-step guide on how to include this library in an XCode
  // project can be found here:
  // https://developer.android.com/kotlin/multiplatform/migrate
  val xcfName = "source:telemetryKit"

  iosX64 { binaries.framework { baseName = xcfName } }

  iosArm64 { binaries.framework { baseName = xcfName } }

  iosSimulatorArm64 { binaries.framework { baseName = xcfName } }

  // Source set declarations.
  // Declaring a target automatically creates a source set with the same name. By default, the
  // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
  // common to share sources between related targets.
  // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
  sourceSets {
    commonMain { dependencies { implementation(libs.kotlin.stdlib) } }

    commonTest { dependencies { implementation(libs.kotlin.test) } }

    androidMain { dependencies {} }

    getByName("androidDeviceTest") {
      dependencies {
        implementation(libs.androidx.core)
        implementation(libs.androidx.junit)
        implementation(libs.androidx.runner)
      }
    }

    iosMain { dependencies {} }
  }
}
