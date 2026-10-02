plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.mavenPublish)
}

android {
  namespace = "com.clerk.telemetry"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
  }
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
  api(libs.okhttp)

  implementation(libs.kotlin.stdlib)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.kotlinx.serialization)
  implementation(projects.source.api)

  // Only the deprecated Ktor overloads in TelemetryCollector and TelemetryModule use Ktor, so it
  // stays off consumers' runtime classpath. Callers of those overloads ship Ktor themselves.
  compileOnly(libs.ktor.client.core)
  compileOnly(libs.ktor.client.negototiation)
  compileOnly(libs.ktor.client.okhttp)
  compileOnly(libs.ktor.serialization.kotlinx.json)

  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.ktor.client.mock)
}
