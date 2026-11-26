package com.clerk.api.session

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.Test

class ReverificationRequiredExceptionTest {

  @Test
  fun `reads a named requirement`() {
    val named =
      mapOf(
        "strict_mfa" to ReverificationConfig.StrictMfa,
        "strict" to ReverificationConfig.Strict,
        "moderate" to ReverificationConfig.Moderate,
        "lax" to ReverificationConfig.Lax,
      )
    for ((name, config) in named) {
      val exception = assertNotNull(parse(""""$name""""))
      assertEquals(config, exception.reverification, name)
    }
  }

  @Test
  fun `reads a custom requirement`() {
    val exception = parse("""{"level": "first_factor", "afterMinutes": 5}""")

    assertEquals(
      ReverificationConfig.Custom(SessionVerification.Level.FIRST_FACTOR, 5),
      exception?.reverification,
    )
  }

  @Test
  fun `keeps the error but drops a requirement it cannot read`() {
    val unreadable =
      listOf(
        """"forever"""",
        """{"level": "third_factor", "afterMinutes": 5}""",
        """{"level": "unknown", "afterMinutes": 5}""",
        """{"level": "first_factor", "afterMinutes": 0}""",
        """{"level": "first_factor", "afterMinutes": -1}""",
        """{"level": "first_factor", "afterMinutes": 1e20}""",
        """{"level": "first_factor", "afterMinutes": "5"}""",
        """{"level": "first_factor"}""",
        "5",
        "null",
      )
    for (reverification in unreadable) {
      val exception = parse(reverification)
      assertNull(assertNotNull(exception, reverification).reverification, reverification)
    }
  }

  @Test
  fun `keeps the error when the body has no metadata`() {
    val exception =
      ReverificationRequiredException.fromResponseBody(
        """{"clerk_error": {"type": "forbidden", "reason": "reverification-error"}}"""
      )

    assertNull(assertNotNull(exception).reverification)
  }

  @Test
  fun `ignores other bodies`() {
    val others =
      listOf(
        """{"clerk_error": {"type": "forbidden", "reason": "missing-permission"}}""",
        """{"clerk_error": {"type": "unauthorized", "reason": "reverification-error"}}""",
        """{"clerk_error": "reverification-error"}""",
        """{"errors": [{"code": "session_reverification_required"}]}""",
        "[]",
        "",
        "not json",
      )
    for (body in others) {
      assertNull(ReverificationRequiredException.fromResponseBody(body), body)
    }
  }

  @Test
  fun `recognizes Clerk's reverification error code`() {
    val failure =
      ClerkResult.httpFailure(
        403,
        ClerkErrorResponse(errors = listOf(Error(code = "session_reverification_required"))),
      )

    val exception = assertNotNull(failure.reverificationRequiredOrNull())

    assertNull(exception.reverification)
  }

  @Test
  fun `returns the exception a failure carries`() {
    val exception = ReverificationRequiredException(ReverificationConfig.Moderate)

    assertSame(exception, ClerkResult.unknownFailure(exception).reverificationRequiredOrNull())
  }

  @Test
  fun `ignores other failures`() {
    val otherCode =
      ClerkResult.apiFailure(
        ClerkErrorResponse(errors = listOf(Error(code = "form_password_incorrect")))
      )

    assertNull(otherCode.reverificationRequiredOrNull())
    assertNull(ClerkResult.unknownFailure(IOException()).reverificationRequiredOrNull())
    assertNull(ClerkResult.httpFailure<ClerkErrorResponse>(403).reverificationRequiredOrNull())
  }

  private fun parse(reverification: String): ReverificationRequiredException? =
    ReverificationRequiredException.fromResponseBody(
      """
      {"clerk_error": {"type": "forbidden", "reason": "reverification-error",
       "metadata": {"reverification": $reverification}}}
      """
        .trimIndent()
    )
}
