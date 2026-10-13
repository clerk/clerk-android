package com.clerk.api.network.serialization

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import java.io.IOException
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ClerkResultTransformTest {
  private val tags: Map<KClass<*>, Any> = mapOf(String::class to "tag")

  @Test
  fun `map transforms success values and keeps tags`() {
    val result = ClerkResult.Success(2, tags).map { it * 10 }

    val success = result as ClerkResult.Success
    assertEquals(20, success.value)
    assertEquals(tags, success.tags)
  }

  @Test
  fun `map returns failures unchanged without calling transform`() {
    val failure = ClerkResult.httpFailure(code = 404, error = "missing")
    var called = false

    val result =
      failure.map<Int, Int, String> {
        called = true
        it
      }

    assertSame(failure, result)
    assertFalse(called)
  }

  @Test
  fun `mapError transforms the error and keeps the other failure details`() {
    val throwable = IOException("boom")
    val failure =
      ClerkResult.Failure(
        error = "bad",
        throwable = throwable,
        code = 422,
        errorType = ClerkResult.Failure.ErrorType.HTTP,
        tags = tags,
      )

    val mapped = failure.mapError { it.length } as ClerkResult.Failure

    assertEquals(3, mapped.error)
    assertSame(throwable, mapped.throwable)
    assertEquals(422, mapped.code)
    assertEquals(ClerkResult.Failure.ErrorType.HTTP, mapped.errorType)
    assertEquals(tags, mapped.tags)
  }

  @Test
  fun `mapError keeps a missing error missing and leaves successes alone`() {
    val unknown: ClerkResult<Int, String> = ClerkResult.unknownFailure(IOException("offline"))
    val success: ClerkResult<Int, String> = ClerkResult.success(1)

    assertNull((unknown.mapError { it.length } as ClerkResult.Failure).error)
    assertSame(success, success.mapError { it.length })
  }

  @Test
  fun `catchingClerkResult converts exceptions to unknown failures`() {
    val exception = IllegalStateException("boom")
    var reported: Exception? = null

    val result =
      catchingClerkResult<Int, ClerkErrorResponse>(onException = { reported = it }) {
        throw exception
      }

    assertSame(exception, (result as ClerkResult.Failure).throwable)
    assertSame(exception, reported)
  }

  @Test
  fun `catchingClerkResult rethrows cancellation`() {
    val cancellation = CancellationException("cancelled")

    val thrown = runCatching {
      catchingClerkResult<Int, ClerkErrorResponse> { throw cancellation }
    }
      .exceptionOrNull()

    assertSame(cancellation, thrown)
  }

  @Test
  fun `localFailure uses the API error shape`() {
    val failure = localFailure(code = "invalid_arguments", longMessage = "Email must be provided")

    assertEquals(ClerkResult.Failure.ErrorType.API, failure.errorType)
    assertEquals(
      Error(
        message = "Email must be provided",
        longMessage = "Email must be provided",
        code = "invalid_arguments",
      ),
      failure.error?.errors?.single(),
    )
  }
}
