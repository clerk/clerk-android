package com.clerk.api

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

public interface NativeCapabilities {
  public val supported: Set<String>
  public suspend fun perform(capability: String, arguments: JsonElement): JsonElement
}

internal class QuickJSWorker(private val output: (JsonElement) -> Unit) {
  private val executor = Executors.newSingleThreadExecutor { Thread(it, "Clerk QuickJS").apply { isDaemon = true } }
  private val dispatcher = executor.asCoroutineDispatcher()
  private val closed = AtomicBoolean(false)
  private var engine = 0L
  private val random = SecureRandom()
  private external fun nativeCreate(): Long
  private external fun nativeEvaluate(handle: Long, source: ByteArray)
  private external fun nativeReceive(handle: Long, message: ByteArray)
  private external fun nativeClose(handle: Long)
  private fun emitBytes(bytes: ByteArray) {
    if (closed.get()) return
    try { output(Json.parseToJsonElement(bytes.decodeToString(throwOnInvalidSequence = true))) }
    catch (_: Exception) { output(buildJsonObject { put("kind", "runtimeError") }) }
  }
  private fun randomBase64(size: Int): ByteArray {
    require(size in 0..65536)
    val bytes = ByteArray(size)
    random.nextBytes(bytes)
    return Base64.getEncoder().encode(bytes)
  }
  suspend fun start(bundle: ByteArray, sha256: String) = withContext(dispatcher) {
    if (closed.get() || engine != 0L) throw CoreException("runtime_already_initialized")
    val actual = MessageDigest.getInstance("SHA-256").digest(bundle).joinToString("") { "%02x".format(it) }
    if (actual != sha256) throw CoreException("bundle_hash_mismatch")
    engine = nativeCreate()
    if (engine == 0L) throw CoreException("engine_unavailable")
    try { nativeEvaluate(engine, bundle) }
    catch (error: Exception) { nativeClose(engine); engine = 0; throw error }
  }
  fun send(message: ByteArray) {
    if (closed.get()) throw CoreException("runtime_unavailable")
    executor.execute {
      if (closed.get() || engine == 0L) return@execute
      try { nativeReceive(engine, message) }
      catch (_: Exception) { output(buildJsonObject { put("kind", "runtimeError") }) }
    }
  }
  fun close() {
    if (!closed.compareAndSet(false, true)) return
    executor.execute {
      if (engine != 0L) { nativeClose(engine); engine = 0 }
      dispatcher.close()
    }
  }
  companion object { init { System.loadLibrary("clerk_quickjs") } }
}

public class QuickJSTransport(private val capabilities: NativeCapabilities, dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : CoreTransport {
  override var receive: ((JsonElement) -> Unit)? = null
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)
  private val jobs = mutableMapOf<String, Job>()
  private val closing = AtomicBoolean(false)
  private var closed = false
  private val worker = QuickJSWorker { value -> scope.launch { route(value) } }
  public suspend fun start(bundle: ByteArray, sha256: String) { worker.start(bundle, sha256) }
  override fun send(message: JsonElement) {
    if (closed) throw CoreException("runtime_unavailable")
    val bytes = message.toString().toByteArray(Charsets.UTF_8)
    if (bytes.size > 16 * 1024 * 1024) throw CoreException("message_too_large")
    worker.send(bytes)
  }
  private fun route(value: JsonElement) {
    if (closed) return
    val message = value.jsonObject
    when (message["kind"]?.requireString()) {
      "hostRequest" -> {
        val id = message.getValue("id").requireString()
        if (jobs.containsKey(id)) return
        val job = scope.launch(start = CoroutineStart.LAZY) {
          val reply = try {
            val result = capabilities.perform(message.getValue("capability").requireString(), message["args"] ?: JsonNull)
            buildJsonObject { put("kind", "hostReply"); put("id", id); put("result", result) }
          } catch (cancelled: CancellationException) { throw cancelled }
          catch (error: Exception) {
            buildJsonObject { put("kind", "hostReply"); put("id", id); put("error", buildJsonObject { put("code", (error as? CoreException)?.code ?: "host_failure") }) }
          }
          jobs.remove(id)
          if (isActive && !closing.get()) send(reply)
        }
        jobs[id] = job
        job.start()
      }
      "hostCancel" -> jobs.remove(message.getValue("id").requireString())?.cancel()
      else -> receive?.invoke(value)
    }
  }
  override fun close() {
    if (!closing.compareAndSet(false, true)) return
    scope.launch {
      closed = true
      jobs.values.forEach(Job::cancel); jobs.clear()
      worker.close()
      scope.cancel()
    }
  }
}
