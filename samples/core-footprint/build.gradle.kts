import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { alias(libs.plugins.android.application) }

android {
  namespace = "com.clerk.footprint"
  ndkVersion = "27.1.12297006"
  compileSdk = libs.versions.compileSdk.get().toInt()
  defaultConfig {
    applicationId = "com.clerk.footprint"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.compileSdk.get().toInt()
    versionCode = 1
    versionName = "1"
    ndk { abiFilters += providers.gradleProperty("footprintAbi").getOrElse("arm64-v8a") }
  }
  flavorDimensions += "core"
  productFlavors {
    create("baseline") {
      dimension = "core"
      applicationIdSuffix = ".baseline"
    }
    create("embedded") {
      dimension = "core"
      applicationIdSuffix = ".embedded"
    }
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      signingConfig = signingConfigs.getByName("debug")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
  }
  // Identical fixture asset in both variants so it does not inflate the SDK delta.
  sourceSets.getByName("main").assets.directories.add("../../NativeCoreTests/Fixtures")
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
  "embeddedImplementation"(project(":source:api"))
}
