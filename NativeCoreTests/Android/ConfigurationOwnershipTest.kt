package com.clerk.api

import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.net.URI
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigurationOwnershipTest {
  @Test
  fun separateConfigurationsKeepCredentialsIsolatedWhenOneOwnerCloses() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val fixtureContext = InstrumentationRegistry.getInstrumentation().context
      val suffix = UUID.randomUUID().toString()
      val keys =
        listOf("first", "second").map {
          "pk_test_" +
            Base64.getEncoder().encodeToString("$it-$suffix.clerk.accounts.dev$".toByteArray())
        }
      val stores = keys.map { AndroidCredentialStorage(context, it) }
      val entered = CompletableDeferred<Unit>()
      val cancelled = CompletableDeferred<Unit>()
      val owners = mutableListOf<Clerk>()
      val requests = List(2) { mutableListOf<JsonObject>() }
      try {
        for (index in keys.indices) {
          stores[index].write("credential-$index")
          val fixtures = PackagedFixtures(fixtureContext)
          if (index == 1)
            fixtures.clientResponse = fixtures.fixtures.getValue("authenticatedClient")
          val capabilities =
            object : NativeCapabilities {
              override val supported = fixtures.supported

              override suspend fun perform(
                capability: String,
                arguments: JsonElement,
              ): JsonElement {
                val args = arguments.jsonObject
                if (capability.startsWith("storage."))
                  assertEquals(keys[index], args.getValue("scope").requireString())
                when (capability) {
                  "storage.read" -> return stores[index].read()?.let(::JsonPrimitive) ?: JsonNull
                  "storage.write" -> {
                    stores[index].write(args.getValue("value").requireString())
                    return JsonNull
                  }
                  "storage.remove" -> {
                    stores[index].remove()
                    return JsonNull
                  }
                }
                if (capability == "http") {
                  requests[index] += args
                  val path = URI(args.getValue("url").requireString()).path
                  if (index == 0 && path.contains("/sign_ins")) {
                    entered.complete(Unit)
                    try {
                      awaitCancellation()
                    } finally {
                      cancelled.complete(Unit)
                    }
                  }
                  val result = fixtures.perform(capability, arguments).jsonObject
                  return JsonObject(
                    result +
                      ("headers" to
                        buildJsonObject {
                          if (!path.endsWith("/environment"))
                            put("authorization", "credential-$index")
                        })
                  )
                }
                return fixtures.perform(capability, arguments)
              }
            }
          owners +=
            Clerk.connect(
              context,
              ClerkConfiguration(keys[index], "clerk-test://sso-callback"),
              capabilities,
            )
        }
        val first = owners[0]
        val second = owners[1]
        val firstAttempt = first.signIn
        val secondAttempt = second.signIn
        val pending = async {
          runCatching { firstAttempt.sso(SignInSSOParams(SignInSSOParamsStrategy.OauthGoogle)) }
        }
        withTimeout(3000) { entered.await() }
        first.close()
        withTimeout(3000) { cancelled.await() }
        assertEquals(
          "runtime_disposed",
          (withTimeout(3000) { pending.await() }.exceptionOrNull() as CoreException).code,
        )
        assertTrue(firstAttempt.isInvalidated)
        assertFalse(secondAttempt.isInvalidated)
        assertEquals("sess_native", second.session?.id)
        assertEquals("user_native", second.user?.id)
        second.signIn.reset()
        assertFalse(second.isInvalidated)
        assertEquals("sess_native", second.session?.id)
        for (index in keys.indices) {
          assertEquals("credential-$index", AndroidCredentialStorage(context, keys[index]).read())
          assertTrue(requests[index].isNotEmpty())
          for (request in requests[index]) {
            assertEquals(
              if (index == 0) "first-$suffix.clerk.accounts.dev"
              else "second-$suffix.clerk.accounts.dev",
              URI(request.getValue("url").requireString()).host,
            )
            if (!URI(request.getValue("url").requireString()).path.endsWith("/environment"))
              assertEquals(
                "credential-$index",
                request.getValue("headers").jsonObject.getValue("authorization").requireString(),
              )
          }
        }
        stores[0].remove()
        assertNull(AndroidCredentialStorage(context, keys[0]).read())
        assertEquals("credential-1", AndroidCredentialStorage(context, keys[1]).read())
      } finally {
        owners.forEach { it.close() }
        for (key in keys) {
          val hash = instanceHash(key)
          for (purpose in AndroidCredentialStorage.Purpose.entries) AtomicFile(
              File(context.noBackupFilesDir, "clerk-core/$hash.${purpose.suffix}")
            )
            .delete()
          KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            deleteEntry("${context.packageName}.clerk.core.v2.$hash")
          }
        }
      }
    }
  }
}
