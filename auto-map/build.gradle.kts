import com.vanniktech.maven.publish.SonatypeHost

plugins {
  id("java-library")
  alias(libs.plugins.jetbrains.kotlin.jvm)
  alias(libs.plugins.ksp)
  alias(libs.plugins.mavenPublish)
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

mavenPublishing {
  coordinates("com.clerk", "automap", "0.0.1")
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
  signAllPublications()

  pom {
    name.set("Clerk AutoMap")
    description.set("Generate maps from data classes.")
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

kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21 } }

dependencies {
  implementation(libs.ksp.api)

  testImplementation(kotlin("test"))
  testImplementation(libs.junit)
  testImplementation(libs.mockito)
}
