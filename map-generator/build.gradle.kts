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
    register<MavenPublication>("auto-map") {
      groupId = "com.clerk"
      artifactId = "auto-map"
      version = "0.1.0"

      from(components["java"])

      pom {
        name = "Auto Map"
        description = "A library for generating maps using KSP."
        url.set("https://github.com/clerk/clerk-android")

        scm {
          connection.set("scm:git@github.com:clerk/clerk-android.git")
          developerConnection.set("scm:git@github.com:clerk/clerk-android.git")
          url.set("https://github.com/clerk/clerk-android")
        }
        licenses {
          license {
            name.set("MIT License")
            url.set("https://opensource.org/licenses/MIT")
          }

          developers {
            developer {
              id.set("clerk")
              name.set("Clerk")
              email.set("support@clerk.dev")
            }
          }
        }
      }
    }
  }
  repositories {
    maven {
      credentials {
        username = "AWLQyWkv"
        password = "sMf3qSD1/KUwVAo0C2nhpFcbPuw7Knw8ZqzImCYS4ygT"
      }
    }
  }
}

kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21 } }

dependencies {
  implementation(libs.ksp.api)

  testImplementation(kotlin("test"))
  testImplementation(libs.junit)
  testImplementation(libs.mockito)
}
