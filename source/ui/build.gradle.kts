import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.dokka)
  id("com.vanniktech.maven.publish")
  alias(libs.plugins.paparazzi)
}

android {
  namespace = "com.clerk.ui"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    debug { isMinifyEnabled = false }
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin { jvmToolchain(17) }

  buildFeatures { compose = true }

  testOptions { unitTests.isIncludeAndroidResources = true }

  kotlin {
    compilerOptions {
      freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
  }
}

// Configure Maven publishing for this module
mavenPublishing {
  coordinates("com.clerk", "clerk-android-ui", libs.versions.clerk.ui.get())

  pom {
    name.set("Clerk Android UI")
    description.set("UI components for Clerk Android SDK")
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

tasks.withType<DokkaTaskPartial>().configureEach {
  dependencies { dokkaPlugin(libs.versioning.plugin) }
  moduleName.set("Clerk Android UI")
  suppressInheritedMembers.set(true)
  dokkaSourceSets.configureEach { reportUndocumented.set(true) }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.tooling)
  implementation(libs.androidx.ui.tooling.preview.android)
  implementation(libs.coil)
  implementation(libs.coil.okhttp)
  implementation(libs.google.libphonenumber)
  implementation(libs.kotlinx.immutable)
  implementation(libs.material)
  implementation(libs.material3)
  implementation(libs.materialKolor)

  compileOnly(projects.clerk.source.api)

  testImplementation(libs.junit)
  testImplementation(projects.clerk.source.api)

  //  testImplementation(libs.paparazzi)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)

  lintChecks(libs.compose.lints)
}
