plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.mavenPublish)
}

extensions.configure<com.android.build.api.dsl.LibraryExtension> {
  namespace = "com.clerk.sdk"
  compileSdk = libs.versions.compileSdk.get().toInt()
  ndkVersion = "27.1.12297006"
  sourceSets.getByName("main").apply {
    java.directories.clear()
    kotlin.directories.apply { clear(); add("../../NativeCore") }
    assets.directories.apply { clear(); add("../../NativeCore/Resources") }
    res.directories.clear()
    manifest.srcFile("../../NativeCore/Android/AndroidManifest.xml")
  }
  sourceSets.getByName("test").kotlin.directories.apply { clear(); add("../../NativeCoreTests/Unit"); add("../../NativeCoreTests/Common") }
  sourceSets.getByName("androidTest").kotlin.directories.apply { clear(); add("../../NativeCoreTests/Android"); add("../../NativeCoreTests/Common") }
  sourceSets.getByName("androidTest").assets.directories.add("../../NativeCoreTests/Fixtures")
  externalNativeBuild { cmake { path = file("../../NativeCore/cpp/CMakeLists.txt"); version = "3.22.1" } }
  compileOptions { isCoreLibraryDesugaringEnabled = true }

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "SDK_VERSION", "\"${property("CLERK_API_VERSION")}\"")
    consumerProguardFiles("consumer-rules.pro")
  }

  testBuildType = providers.gradleProperty("clerkTestBuildType").getOrElse("debug")
  testOptions { unitTests.isIncludeAndroidResources = true }

  buildTypes {
    debug { isMinifyEnabled = false }
    release { isMinifyEnabled = false }
  }

  buildFeatures { buildConfig = true }
  packaging {
    resources {
      excludes += "/META-INF/LICENSE.md"
      excludes += "/META-INF/NOTICE.md"
      excludes += "/META-INF/LICENSE-notice.md"
      excludes += "/META-INF/NOTICE"
      excludes += "/META-INF/LICENSE*"
    }
  }
}

dokka {
  moduleName.set("Clerk Android API")
  dokkaSourceSets.configureEach {
    includes.from(listOf("module.md"))
    reportUndocumented.set(true)
  }
  dokkaPublications.configureEach { suppressInheritedMembers.set(true) }
}

mavenPublishing {
  coordinates("com.clerk", "clerk-android-api", property("CLERK_API_VERSION") as String)
  publishToMavenCentral()
  // Skip signing for local publishing: ./gradlew publishToMavenLocal
  // -PRELEASE_SIGNING_ENABLED=false
  if (providers.gradleProperty("RELEASE_SIGNING_ENABLED").orNull != "false") {
    signAllPublications()
  }
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

dependencies {
  api(libs.kotlinx.serialization)
  api(libs.kotlinx.coroutines)
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
  implementation(libs.androidx.browser)
  implementation(libs.androidx.biometric)
  implementation(libs.google.identity)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.playServicesAuth)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.okhttp)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation("androidx.test:runner:1.7.0")
  androidTestImplementation("androidx.test.ext:junit:1.3.0")
  androidTestImplementation(libs.kotlinx.coroutines.test)
  dokkaPlugin(libs.versioning.plugin)
}
