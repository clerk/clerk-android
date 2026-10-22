package com.clerk.detekt

import dev.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

class SwallowedCancellationTest {
  private val rule = SwallowedCancellation()

  @Test
  fun `flags broad catches and runCatching in suspend code`() {
    val findings =
      rule.lint(
        """
        suspend fun a() {
          try { load() } catch (e: Exception) { log(e) }
          runCatching { load() }.onFailure { log(it) }
        }

        fun b(scope: CoroutineScope) {
          scope.launch { try { load() } catch (t: Throwable) { log(t) } }
          scope.launch { withContext(Dispatchers.IO) { runCatching { load() } } }
        }
        """
          .trimIndent()
      )

    assertEquals(4, findings.size)
  }

  @Test
  fun `allows code that rethrows cancellation`() {
    val findings =
      rule.lint(
        """
        suspend fun a() {
          try { load() } catch (e: CancellationException) { throw e } catch (e: Exception) { log(e) }
          try { load() } catch (e: Exception) { if (e is CancellationException) throw e }
          try { load() } catch (e: Exception) { cleanup(); throw e }
          runCatching { load() }.onFailure { if (it is CancellationException) throw it }
          catchingClerkResult { load() }
          try { load() } catch (e: CancellationException) { runCatching { undo() }; throw e }
        }
        """
          .trimIndent()
      )

    assertEquals(0, findings.size)
  }

  @Test
  fun `ignores code that cannot be cancelled`() {
    val findings =
      rule.lint(
        """
        suspend fun a() {
          val sessions = runCatching { Clerk.client.sessions }.getOrNull()
          try { cache = Clerk.client.lastActiveSessionId } catch (e: Exception) { log(e) }
          withContext(NonCancellable) {
            runCatching { revoke() }
            try { delete() } catch (e: Exception) { log(e) }
          }
        }
        """
          .trimIndent()
      )

    assertEquals(0, findings.size)
  }

  @Test
  fun `ignores non-suspend code`() {
    val findings =
      rule.lint(
        """
        fun a() {
          try { parse() } catch (e: Exception) { log(e) }
          runCatching { parse() }
        }

        suspend fun b() {
          val listener = object : Listener { override fun on() { runCatching { parse() } } }
          fun local() = runCatching { parse() }
        }
        """
          .trimIndent()
      )

    assertEquals(0, findings.size)
  }
}
