package com.clerk.api

import android.os.Bundle
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreBenchmarkTest {
  @Test fun measurePackagedCoreWhenRequested() = runBlocking {
    assumeTrue(InstrumentationRegistry.getArguments().getString("clerkBenchmark") == "true")
    withTimeout(120000) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val configuration = ClerkConfiguration(key, "clerk-test://sso-callback")
      val startup = mutableListOf<Double>()
      val calls = mutableListOf<Double>()
      val memory = mutableListOf<JsonObject>()
      memory += memorySample("before")
      withContext(Dispatchers.Main.immediate) {
        repeat(12) { index ->
          val capabilities = PackagedFixtures(instrumentation.context)
          val before = SystemClock.elapsedRealtimeNanos()
          val clerk = Clerk.connect(instrumentation.targetContext, configuration, capabilities)
          startup += (SystemClock.elapsedRealtimeNanos() - before) / 1e6
          try {
            check(clerk.loaded)
            memory += memorySample("connected-$index")
            clerk.signIn.reset()
            val requestCount = capabilities.requests.size
            repeat(25) {
              val prior = clerk.signIn.emailCode
              val start = SystemClock.elapsedRealtimeNanos()
              clerk.signIn.reset()
              calls += (SystemClock.elapsedRealtimeNanos() - start) / 1e6
              check(prior.isInvalidated && capabilities.requests.size == requestCount)
            }
          } finally { clerk.close() }
        }
      }
      memory += memorySample("after-close")
      val report = buildJsonObject {
        put("schemaVersion", 1)
        put("platform", "Android ${Build.VERSION.RELEASE} API ${Build.VERSION.SDK_INT}")
        put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
        put("abis", JsonArray(Build.SUPPORTED_ABIS.map(::JsonPrimitive)))
        put("engine", "QuickJS-ng v0.15.1")
        put("buildType", if (com.clerk.sdk.BuildConfig.DEBUG) "debug instrumentation" else "release library in instrumentation")
        put("coreRevision", BundledCore.coreRevision)
        put("bundleSHA256", BundledCore.sha256)
        put("fixture", "packaged FAPI JSON; in-memory HTTP and storage; no service or platform prompts")
        put("startupDefinition", "connect: bundled asset read, hash verification, fresh engine, core load, fixture HTTP and initial native projection")
        put("operationDefinition", "generated signIn.reset: local core mutation, handle invalidation and full state projection; no HTTP")
        put("startupMilliseconds", JsonArray(startup.map(::JsonPrimitive)))
        put("localResetMilliseconds", JsonArray(calls.map(::JsonPrimitive)))
        put("startupSummary", summary(startup))
        put("localResetSummary", summary(calls))
        put("processMemorySamples", JsonArray(memory))
        put("memoryDefinition", "process-wide sampled Android Debug.MemoryInfo PSS and native heap; includes test runner and libraries, not isolated engine memory")
        put("limitations", JsonArray(listOf("repeated fresh engines in one warm process", "first startup sample reported separately", "not a cold application launch", "fixture timing excludes real HTTP and secure storage", "instrumentation results include the test runner and are not cold-app measurements", "compare against documented release budgets on physical devices").map(::JsonPrimitive)))
      }
      val output = File(instrumentation.targetContext.filesDir, "clerk-core-benchmark.json")
      output.writeText(report.toString())
      instrumentation.sendStatus(0, Bundle().apply { putString("clerkBenchmarkReport", report.toString()) })
      println("BENCHMARK: ${startup.size} starts, ${calls.size} local resets; ${output.name}")
    }
  }

  private fun memorySample(phase: String): JsonObject {
    val info = Debug.MemoryInfo()
    Debug.getMemoryInfo(info)
    return buildJsonObject {
      put("phase", phase)
      put("totalPssKiB", info.totalPss)
      put("nativePssKiB", info.nativePss)
      put("dalvikPssKiB", info.dalvikPss)
      put("nativeHeapAllocatedBytes", Debug.getNativeHeapAllocatedSize())
    }
  }

  private fun summary(values: List<Double>): JsonObject {
    val sorted = values.sorted()
    return buildJsonObject {
      put("first", values.first())
      put("minimum", sorted.first())
      put("median", sorted[sorted.size / 2])
      put("p95", sorted[kotlin.math.ceil(sorted.size * 0.95).toInt() - 1])
      put("maximum", sorted.last())
    }
  }
}
