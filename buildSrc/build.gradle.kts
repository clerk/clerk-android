plugins {
  `kotlin-dsl`
}

repositories {
  google()
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(gradleApi())
  implementation(localGroovy())
  implementation("com.autonomousapps:dependency-analysis-gradle-plugin:1.33.0")
}

