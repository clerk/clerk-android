package com.clerk.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BuildScriptEditTest {
  private val script =
    """
    plugins { id("com.android.library") }

    dependencies {
      implementation(platform(libs.compose.bom))
      implementation(libs.androidx.appcompat)
      implementation(libs.okhttp)

      testImplementation(libs.junit)
      testImplementation(libs.mockk)

      ksp(libs.processor)
    }
    """
      .trimIndent()

  @Test
  fun `removes a declaration and adds one in order`() {
    val edit = BuildScriptEdit(script.lines())

    edit.remove("testImplementation", listOf(Regex.escape("libs.mockk")))
    edit.add("testImplementation(libs.androidx.core)")

    assertEquals(
      script
        .replace("  testImplementation(libs.mockk)\n", "")
        .replace(
          "  testImplementation(libs.junit)",
          "  testImplementation(libs.androidx.core)\n  testImplementation(libs.junit)",
        ) + "\n",
      edit.text(),
    )
  }

  @Test
  fun `drops the blank line left by removing a whole group`() {
    val edit = BuildScriptEdit(script.lines())

    edit.remove("ksp", listOf(Regex.escape("libs.processor")))

    assertEquals(script.replace("\n\n  ksp(libs.processor)", "") + "\n", edit.text())
  }

  @Test
  fun `starts a new group for a configuration with no declarations`() {
    val edit = BuildScriptEdit(script.lines())

    edit.add("debugImplementation(libs.leakcanary)")

    assertEquals(
      script.replace(
        "  ksp(libs.processor)\n",
        "  ksp(libs.processor)\n\n  debugImplementation(libs.leakcanary)\n",
      ) + "\n",
      edit.text(),
    )
  }

  @Test
  fun `adds after the group's last one-line declaration, not inside a multi-line one`() {
    val multiLine =
      script.replace(
        "  testImplementation(libs.mockk)\n",
        "  testImplementation(libs.mockk)\n  testImplementation(libs.robolectric) {\n    exclude(group = \"x\")\n  }\n",
      )
    val edit = BuildScriptEdit(multiLine.lines())

    edit.add("testImplementation(libs.turbine)")

    assertEquals(
      multiLine.replace(
        "  testImplementation(libs.mockk)\n",
        "  testImplementation(libs.mockk)\n  testImplementation(libs.turbine)\n",
      ) + "\n",
      edit.text(),
    )
  }

  @Test
  fun `reports a declaration it cannot find`() {
    assertFalse(BuildScriptEdit(script.lines()).remove("api", listOf(Regex.escape("libs.okhttp"))))
  }
}
