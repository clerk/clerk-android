import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.plugin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.mavenPublish)
  jacoco
}

android {
  namespace = "com.clerk.sdk"
  compileSdk = 36

  defaultConfig {
    minSdk = 24
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "SDK_VERSION", "\"${libs.versions.clerk.sdk.get()}\"")
  }

  buildTypes {
    debug { 
      isMinifyEnabled = false 
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
    }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures { buildConfig = true }
}

tasks.withType<DokkaTaskPartial>().configureEach {
  dependsOn(tasks.named("kspDebugKotlin"))
  dependsOn(tasks.named("kspReleaseKotlin"))
  dependencies { dokkaPlugin(libs.versioning.plugin) }
  moduleName.set("Clerk Android")
  suppressInheritedMembers.set(true)
  dokkaSourceSets.configureEach {
    includes.from(listOf("module.md"))
    reportUndocumented.set(true)
  }
}

dependencies {
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
  implementation(libs.kotlinx.serialization)
  implementation(libs.ksp.api)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging)
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx)

  testImplementation(kotlin("test"))
  testImplementation(libs.androidx.arch.test)
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

// JaCoCo configuration
tasks.register<JacocoReport>("jacocoTestReport") {
  group = "verification"
  description = "Generate Jacoco coverage reports for the debug build."

  dependsOn("testDebugUnitTest")
  
  reports {
    xml.required.set(true)
    html.required.set(true)
  }

  val fileFilter = listOf(
    "**/R.class",
    "**/R\$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    "**/*\$WhenMappings.*",
    "**/*\$serializer.*",
    "**/*\$\$serializer.*"
  )

  val debugTree = fileTree("${buildDir}/tmp/kotlin-classes/debug") {
    exclude(fileFilter)
  }

  val mainSrc = "${project.projectDir}/src/main/java"
  val kotlinSrc = "${project.projectDir}/src/main/kotlin"

  sourceDirectories.setFrom(files(mainSrc, kotlinSrc))
  classDirectories.setFrom(files(debugTree))
  executionData.setFrom(fileTree(buildDir) {
    include("**/*.exec", "**/*.ec")
  })
}

// Ensure jacocoTestReport runs after tests
tasks.named("check") {
  dependsOn("jacocoTestReport")
}
