plugins {
  id("java-library")
  alias(libs.plugins.jetbrains.kotlin.jvm)
  alias(libs.plugins.ksp)
  id("maven-publish")
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21

  withSourcesJar()
  withJavadocJar()
}

publishing {
  publications {
    register<MavenPublication>("mapGenerator") {
      groupId = "com.clerk"
      artifactId = "map-generator"
      version = "0.1.0"

      from(components["java"])
    }
  }
}

kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21 } }

dependencies { implementation(libs.ksp.api) }
