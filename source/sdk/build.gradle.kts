plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.plugin.serialization)
  id("maven-publish")
}

android {
  namespace = "com.clerk"
  compileSdk = 35

  defaultConfig {
    minSdk = 24

    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
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

dependencies { implementation(libs.kotlinx.serialization) }
