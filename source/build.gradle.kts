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
      isTestCoverageEnabled = true
    }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures { buildConfig = true }
  
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
      all {
        it.systemProperty("robolectric.enabledSdks", "34")
        it.systemProperty("robolectric.offline", "true")
        it.jvmArgs("-noverify")
      }
    }
  }
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

// Simple JaCoCo configuration for Robolectric tests
tasks.register<JacocoReport>("jacocoTestReport") {
  dependsOn("testDebugUnitTest")
  group = "verification"
  description = "Generate Jacoco coverage reports for unit tests"

  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(false)
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
    "**/*\$\$serializer.*",
    "**/*\$Companion.*"
  )

  val javaClasses = fileTree("${layout.buildDirectory.get().asFile}/intermediates/javac/debug/classes") {
    exclude(fileFilter)
  }
  
  val kotlinClasses = fileTree("${layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug") {
    exclude(fileFilter)
  }

  classDirectories.setFrom(files(javaClasses, kotlinClasses))
  
  sourceDirectories.setFrom(files(
    "${project.projectDir}/src/main/java",
    "${project.projectDir}/src/main/kotlin"
  ))
  
  // Look for execution data in multiple locations
  val executionDataFiles = files(
    "${layout.buildDirectory.get().asFile}/jacoco/testDebugUnitTest.exec",
    "${layout.buildDirectory.get().asFile}/outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
    "${layout.buildDirectory.get().asFile}/outputs/code_coverage/debugUnitTest/testDebugUnitTest.exec"
  ).filter { it.exists() }
  
  executionData.setFrom(executionDataFiles)
  
  doFirst {
    println("JaCoCo configuration:")
    println("Source dirs: ${sourceDirectories.files}")
    println("Class dirs: ${classDirectories.files}")
    println("Execution data: ${executionData.files}")
  }
}

// Ensure test report runs after check
tasks.named("check") {
  dependsOn("jacocoTestReport")
}
