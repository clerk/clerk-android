plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.android.lint)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.mavenPublish)
}

mavenPublishing {
  coordinates("com.clerk", "clerk-android-telemetry", libs.versions.clerk.telemetry.get())
  publishToMavenCentral()

  pom {
    name.set("Clerk Android Telemetry")
    description.set("Telemetry module for Clerk Android SDK")
    inceptionYear.set("2025")
    url.set("https://github.com/clerk/clerk-android")
    licenses {
      license {
        name.set("MIT License")
        url.set("https://github.com/clerk/clerk-android/blob/main/LICENSE")
        distribution.set("https://github.com/clerk/clerk-android/blob/main/LICENSE")
      }
    }
    developers {
      developer {
        id.set("clerk")
        name.set("Clerk")
        url.set("https://clerk.com")
      }
    }
    scm {
      url.set("https://github.com/clerk/clerk-android")
      connection.set("scm:git:git://github.com/clerk/clerk-android.git")
      developerConnection.set("scm:git:ssh://github.com:clerk/clerk-android.git")
    }
  }
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
        implementation(projects.source.api)
      }
    }

    getByName("androidDeviceTest") {
      dependencies {
        implementation(libs.androidx.core)
        implementation(libs.androidx.runner)
        implementation(libs.junit)
      }
    }
  }
}
