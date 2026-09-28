import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins { alias(libs.plugins.jetbrains.kotlin.jvm) }

kotlin {
  jvmToolchain(libs.versions.jvmTarget.get().toInt())
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    apiVersion.set(KotlinVersion.KOTLIN_2_0)
  }
}

dependencies {
  compileOnly(libs.detekt.api)
  testImplementation(libs.detekt.api)
  testImplementation(libs.detekt.test)
  testImplementation(libs.kotlin.test.junit)
}
