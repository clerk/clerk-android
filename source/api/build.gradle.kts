import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.mavenPublish)
}

android {
  namespace = "com.clerk.sdk"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    buildConfigField("String", "SDK_VERSION", "\"${libs.versions.clerk.api.get()}\"")
  }

  testOptions { unitTests.isIncludeAndroidResources = true }

  buildTypes {
    debug { isMinifyEnabled = false }
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
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

tasks.withType<DokkaTaskPartial>().configureEach {
  dependsOn(tasks.named("kspDebugKotlin"))
  dependsOn(tasks.named("kspReleaseKotlin"))
  dependencies { dokkaPlugin(libs.versioning.plugin) }
  moduleName.set("Clerk Android API")
  suppressInheritedMembers.set(true)
  dokkaSourceSets.configureEach {
    includes.from(listOf("module.md"))
    reportUndocumented.set(true)
  }
}

mavenPublishing {
  coordinates("com.clerk", "clerk-android-api", libs.versions.clerk.api.get())
  publishToMavenCentral()
  signAllPublications()
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

  implementation(platform(libs.compose.bom))
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.browser)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.lifecycle)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.lifecycle.viewmodel)
  implementation(libs.androidx.playServicesAuth)
  implementation(libs.chucker.debug)
  implementation(libs.clerk.automap.annotations)
  implementation(libs.google.identity)
  implementation(libs.google.playIntegrity)
  implementation(libs.jwt.decode)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.kotlinx.datetime)
  implementation(libs.ksp.api)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging)
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx)

  compileOnly(libs.androidx.compose.foundation)

  testImplementation(platform(libs.compose.bom))
  testImplementation(kotlin("test"))
  testImplementation(libs.androidx.appcompat)
  testImplementation(libs.androidx.arch.test)
  testImplementation(libs.androidx.compose.foundation)
  testImplementation(libs.core.ktx)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockito)
  testImplementation(libs.mockk)
  testImplementation(libs.robolectric)

  androidTestImplementation(libs.androidx.arch.test)
  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(libs.mockito)
  androidTestImplementation(libs.mockk)
  androidTestImplementation(libs.robolectric)

  ksp(libs.clerk.automap.processor)
}
