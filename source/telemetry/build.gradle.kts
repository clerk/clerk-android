plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.mavenPublish)
}

android {
  namespace = "com.clerk.telemetry"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }
}

mavenPublishing {
  coordinates("com.clerk", "clerk-android-telemetry", property("CLERK_TELEMETRY_VERSION") as String)
  publishToMavenCentral()
  if (providers.gradleProperty("RELEASE_SIGNING_ENABLED").orNull != "false") {
    signAllPublications()
  }
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

dependencies {
  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.kotlinx.serialization)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.negototiation)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.okhttp)
  implementation(projects.source.api)

  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.ktor.client.mock)
}
