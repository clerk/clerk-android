plugins {
  id("java-library")
  alias(libs.plugins.jetbrains.kotlin.jvm)
  alias(libs.plugins.kotlin.plugin.serialization)
  id("maven-publish")
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

afterEvaluate {
  publishing {
    publications {
      register<MavenPublication>("maven") {
        groupId = "com.clerk"
        artifactId = "clerk-serializer"
        version = "0.1.0"

        from(components["java"])

        // Don't try to manually modify the POM - let Gradle handle it
      }
    }
  }
}

kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21 } }

dependencies {
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx)
  implementation(libs.statelyCollections)
}
