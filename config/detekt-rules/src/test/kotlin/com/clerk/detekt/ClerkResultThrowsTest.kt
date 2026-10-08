package com.clerk.detekt

import dev.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

class ClerkResultThrowsTest {
  private val rule = ClerkResultThrows()

  @Test
  fun `flags throwing constructs in public ClerkResult functions`() {
    val findings =
      rule.lint(
        """
        suspend fun a(x: String?): ClerkResult<String, ClerkErrorResponse> {
          requireNotNull(x)
          check(x.isNotEmpty())
          val y = x!!
          if (y == "e") error("boom")
          throw IllegalStateException(y)
        }

        class Service {
          fun b(): ClerkResult.Failure<Nothing> = require(false) { "no" }.let { TODO() }
        }

        fun c(): ClerkResult<String, Nothing> {
          fun local(): Nothing = error("boom")
          return local()
        }
        """
          .trimIndent()
      )

    assertEquals(8, findings.size)
  }

  @Test
  fun `ignores non-public functions and other return types`() {
    val findings =
      rule.lint(
        """
        private fun a(): ClerkResult<String, Nothing> = error("boom")
        internal fun b(): ClerkResult<String, Nothing> = error("boom")
        internal class C { fun c(): ClerkResult<String, Nothing> = error("boom") }
        fun d(): String = error("boom")
        """
          .trimIndent()
      )

    assertEquals(0, findings.size)
  }

  @Test
  fun `does not attribute members of nested or anonymous classes to the enclosing function`() {
    val findings =
      rule.lint(
        """
        fun a(): ClerkResult<String, Nothing> {
          val task = object : Runnable { override fun run() = error("boom") }
          class Local { fun b(): String = error("boom") }
          return ClerkResult.success("ok")
        }
        """
          .trimIndent()
      )

    assertEquals(0, findings.size)
  }

  @Test
  fun `allows rethrowing cancellation and code guarded by broad catches`() {
    val findings =
      rule.lint(
        """
        suspend fun a(): ClerkResult<String, Nothing> {
          return try {
            ClerkResult.success(load()!!)
          } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
          } catch (e: Exception) {
            ClerkResult.unknownFailure(e)
          }
        }

        fun b(): ClerkResult<String, Nothing> = catchingClerkResult { error("caught") }

        fun c(): ClerkResult<String, Nothing> =
          catchingClerkResult(onException = { log(it) }) { ClerkResult.success(load()!!) }
        """
          .trimIndent()
      )

    assertEquals(0, findings.size)
  }

  @Test
  fun `flags throws in catch blocks and narrow try blocks`() {
    val findings =
      rule.lint(
        """
        fun a(): ClerkResult<String, Nothing> {
          return try {
            error("not caught by IOException")
          } catch (e: java.io.IOException) {
            throw IllegalStateException(e)
          }
        }
        """
          .trimIndent()
      )

    assertEquals(2, findings.size)
  }

  @Test
  fun `flags throwing default argument expressions`() {
    val findings =
      rule.lint(
        """
        fun a(id: String = error("missing")): ClerkResult<String, Nothing> =
          ClerkResult.success(id)

        fun b(id: String = catchingClerkResult { error("caught") }): ClerkResult<String, Nothing> =
          ClerkResult.success(id)
        """
          .trimIndent()
      )

    assertEquals(1, findings.size)
  }
}
