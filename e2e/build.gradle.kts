import groovy.json.JsonSlurper

private val e2eKey = "E2E_CLERK_PUBLISHABLE_KEY"
private val e2eKeyNameProperty = "E2E_CLERK_KEY_NAME"

private val e2eKeyName: String? =
  (project.findProperty(e2eKeyNameProperty) as String?)?.trim()?.takeIf { it.isNotEmpty() }

private fun publishableKeyFromKeysFile(keyName: String): String {
  val keysFile = rootProject.file(".keys.json")
  check(keysFile.exists()) {
    "$e2eKeyNameProperty=$keyName requires .keys.json at the repository root."
  }
  val keysJson = providers.fileContents(layout.settingsDirectory.file(".keys.json")).asText.get()
  val keys = JsonSlurper().parseText(keysJson) as Map<*, *>
  val entry = keys[keyName] as? Map<*, *>
  val publishableKey = (entry?.get("pk") as? String)?.trim()
  check(!publishableKey.isNullOrEmpty()) { "Configure '$keyName.pk' in .keys.json." }
  return publishableKey
}

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

if (libs.versions.agp.get().substringBefore(".").toInt() < 9) {
  apply(plugin = "org.jetbrains.kotlin.android")
}

android {
  namespace = "com.clerk.e2e"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.clerk.e2e"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.compileSdk.get().toInt()

    val keyValue =
      e2eKeyName?.let(::publishableKeyFromKeysFile)
        ?: project.findProperty(e2eKey) as String?
        ?: "pk_test_placeholder_for_e2e"
    buildConfigField("String", e2eKey, "\"${keyValue}\"")
    buildConfigField("String", e2eKeyNameProperty, "\"${e2eKeyName ?: "default"}\"")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.activity.compose)
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.lifecycle.viewmodel)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.core.ktx)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.material3)
  implementation(projects.source.api)
  implementation(projects.source.ui)

  debugImplementation(libs.androidx.ui.tooling)

  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
}

tasks.matching { it.name.startsWith("dokka") }.configureEach { enabled = false }
