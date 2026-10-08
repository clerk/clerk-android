plugins {
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
    buildConfigField("String", "SDK_VERSION", "\"${property("CLERK_API_VERSION")}\"")
    consumerProguardFiles("consumer-rules.pro")
  }

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

kotlin { explicitApi() }

tasks.withType<Test>().configureEach {
  // Robolectric accesses FileDescriptor internals when initializing Android shared memory.
  jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
}

dokka {
  moduleName.set("Clerk Android API")
  dokkaSourceSets.configureEach {
    includes.from(listOf("module.md"))
    reportUndocumented.set(true)
  }
  dokkaPublications.configureEach { suppressInheritedMembers.set(true) }
}

tasks
  .matching { it.name.startsWith("dokkaGenerate") }
  .configureEach {
    dependsOn(tasks.named("kspDebugKotlin"))
    dependsOn(tasks.named("kspReleaseKotlin"))
  }

// AutoMap emits public declarations without a modifier, which explicit API mode rejects. Its
// non-public output always carries a modifier, so a bare top-level `fun` is a public one.
// Remove once clerk/AutoMap emits `public` itself.
tasks
  .matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }
  .configureEach {
    doLast {
      outputs.files.asFileTree
        .matching { include("**/*.kt") }
        .forEach { file ->
          val text = file.readText()
          val explicit = text.replace(Regex("^fun ", RegexOption.MULTILINE), "public fun ")
          if (explicit != text) file.writeText(explicit)
        }
    }
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
    name.set("Clerk Android API")
    description.set("Core API client for the Clerk Android SDK: authentication, sessions and users")
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
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.browser)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.playServicesAuth)
  implementation(libs.clerk.automap.annotations)
  implementation(libs.google.identity)
  implementation(libs.jwt.decode)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging)
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx)

  compileOnly(libs.androidx.compose.foundation)

  testImplementation(platform(libs.compose.bom))
  testImplementation(libs.androidx.appcompat)
  testImplementation(libs.androidx.arch.test)
  testImplementation(libs.androidx.compose.foundation)
  testImplementation(libs.core.ktx)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.test.junit)
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

  dokkaPlugin(libs.versioning.plugin)

  ksp(libs.clerk.automap.processor)
}
