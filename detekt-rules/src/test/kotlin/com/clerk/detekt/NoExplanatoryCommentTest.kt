package com.clerk.detekt

import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class NoExplanatoryCommentTest {
  private val rule = NoExplanatoryComment()

  @Test
  fun `reports line block and trailing comments`() {
    val code =
      """
      class Foo {
        // explains the next line
        val a = 1 // trailing
        /* block */
        val b = 2
      }
      """
        .trimIndent()

    assertEquals(listOf(2, 3, 4), rule.lint(code).map { it.location.source.line })
  }

  @Test
  fun `allows kdoc region markers and slashes in strings`() {
    val code =
      """
      /** Documented. */
      class Foo {
        // region Properties
        val url = "https://clerk.com"
        // endregion
      }
      """
        .trimIndent()

    assertEquals(0, rule.lint(code).size)
  }
}
