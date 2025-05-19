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

extra["PUBLISH_GROUP_ID"] = "com.clerk"

extra["PUBLISH_VERSION"] = "0.1.0"

extra["POM_ARTIFACT_ID"] = "auto-map"

apply("${rootProject.projectDir}/scripts/publish-module.gradle")

kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21 } }

dependencies {
  implementation(libs.ksp.api)

  testImplementation(kotlin("test"))
  testImplementation(libs.junit)
  testImplementation(libs.mockito)
}
