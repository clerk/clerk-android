// Repo-specific detekt rules. Wired into every module through `detektPlugins` in the root build.
plugins { alias(libs.plugins.jetbrains.kotlin.jvm) }

dependencies {
  compileOnly(libs.detekt.api)

  testImplementation(libs.detekt.api)
  testImplementation(libs.detekt.test)
  testImplementation(libs.junit)
}
