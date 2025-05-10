plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.plugin.serialization)
  id("maven-publish")
}

android {
  namespace = "com.clerk.sdk"
  compileSdk = 35

  defaultConfig {
    minSdk = 24

    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
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

    repositories {
      mavenLocal()
      maven {
        name = "clerk-android-maven"
        url = uri(layout.buildDirectory.dir("repo"))
      }
    }
  }
}

dependencies {
  implementation(libs.androidx.lifecycle)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.serialization)
}
