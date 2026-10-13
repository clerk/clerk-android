plugins { alias(libs.plugins.jetbrains.kotlin.jvm) }

dependencies {
  compileOnly(libs.detekt.api)

  testImplementation(libs.detekt.api)
  testImplementation(libs.detekt.test)
  testImplementation(libs.junit)
}
