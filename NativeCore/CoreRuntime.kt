package com.clerk.api

import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*

public enum class CoreFailureKind { Clerk, Rejection, Bridge, Cancelled }

public class CoreException(public val code: String, override val message: String = "The operation could not be completed.", public val details: JsonElement? = null, public val kind: CoreFailureKind = CoreFailureKind.Bridge, public val errors: List<ClerkAPIError> = emptyList(), public val passkeyStage: String? = null, public val status: Int? = null, public val retryAfter: Double? = null, public val clerkTraceId: String? = null) : Exception(message) {
  override fun getLocalizedMessage(): String = errors.firstOrNull()?.let { it.longMessage ?: it.message } ?: message
  internal companion object {
    fun fromJson(value: JsonElement, runtime: CoreRuntime): CoreException {
      val v = value.jsonObject
      return CoreException(v.getValue("code").requireString(), v["message"]?.requireString() ?: "The operation could not be completed.", v["errors"], CoreFailureKind.entries.firstOrNull { it.name.equals(v["kind"]?.requireString(), ignoreCase = true) } ?: CoreFailureKind.Bridge, v["errors"]?.jsonArray?.map { ClerkAPIError.fromJson(it, runtime) } ?: emptyList(), v["passkeyStage"]?.requireString(), v["status"]?.jsonPrimitive?.intOrNull, v["retryAfter"]?.jsonPrimitive?.doubleOrNull, v["clerkTraceId"]?.requireString())
    }
  }
}

public data class ResourceHandle(public val id: String, public val generation: Long, public val type: String) {
  public fun toJson(): JsonElement = buildJsonObject { put("id", id); put("generation", generation); put("type", type) }
  public companion object {
    public fun fromJson(value: JsonElement): ResourceHandle {
      val v = value.jsonObject
      val generation = v.getValue("generation").jsonPrimitive.long
      require(generation >= 0)
      return ResourceHandle(v.getValue("id").requireString(), generation, v.getValue("type").requireString())
    }
    public fun fromReference(value: JsonElement): ResourceHandle = fromJson(value.jsonObject.getValue("\$ref"))
  }
}

public interface CoreResource {
  public val handle: ResourceHandle
  public val context: ResourceContext
  public val isInvalidated: Boolean
  public fun prepare(value: JsonElement): Any
}

public interface CoreTransport : AutoCloseable {
  public var receive: ((JsonElement) -> Unit)?
  public fun send(message: JsonElement)
}

public class ResourceContext internal constructor(runtime: CoreRuntime, private val handle: ResourceHandle, ownsRuntime: Boolean) {
  private val owner = WeakReference(runtime)
  private val ownedRuntime = if (ownsRuntime) runtime else null
  @Volatile private var cached: Any? = null
  public fun requireRuntime(): CoreRuntime = ownedRuntime ?: owner.get() ?: throw CoreException("runtime_unavailable")
  public fun isInvalidated(handle: ResourceHandle): Boolean = owner.get()?.isInvalidated(handle) ?: true
  @Suppress("UNCHECKED_CAST") public fun <T> state(handle: ResourceHandle): T = (owner.get()?.stateIfPresent(handle) ?: cached) as? T ?: throw CoreException("state_unavailable")
  internal fun store(state: Any) { cached = state }
}

private object RuntimeCleanup {
  private val queue = java.lang.ref.ReferenceQueue<CoreRuntime>()
  private class Cleanup(runtime: CoreRuntime, val transport: CoreTransport, val revisions: MutableStateFlow<Long>) : WeakReference<CoreRuntime>(runtime, queue)
  private val pending = java.util.concurrent.ConcurrentHashMap.newKeySet<Cleanup>()
  init {
    Thread({
      while (true) {
        try {
          val cleanup = queue.remove() as Cleanup
          pending.remove(cleanup)
          cleanup.transport.close()
          cleanup.revisions.value += 1
        } catch (_: InterruptedException) { return@Thread }
      }
    }, "Clerk runtime cleanup").apply { isDaemon = true; start() }
  }
  fun register(runtime: CoreRuntime, transport: CoreTransport, revisions: MutableStateFlow<Long>) { pending.add(Cleanup(runtime, transport, revisions)) }
}

internal object ResourceCleanup {
  private val queue = java.lang.ref.ReferenceQueue<CoreResource>()
  class Reference(value: CoreResource, runtime: CoreRuntime) : WeakReference<CoreResource>(value, queue) {
    val handle = value.handle
    val owner = WeakReference(runtime)
  }
  init {
    Thread({
      while (true) {
        try { val reference = queue.remove() as Reference; reference.owner.get()?.release(reference) }
        catch (_: InterruptedException) { return@Thread }
      }
    }, "Clerk resource cleanup").apply { isDaemon = true; start() }
  }
  fun reference(value: CoreResource, runtime: CoreRuntime): Reference = Reference(value, runtime)
}

public class CoreRuntime(private val transport: CoreTransport, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) : AutoCloseable {
  private data class Snapshot(val revision: Long = -1, val epoch: Long = 0, val roots: Map<String, ResourceHandle> = emptyMap(), val states: Map<ResourceHandle, Any> = emptyMap(), val available: Boolean = true)
  @Volatile private var snapshot = Snapshot()
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)
  private val pending = mutableMapOf<String, (Result<JsonElement>) -> Unit>()
  private var completionLeases: List<CoreResource> = emptyList()
  private val resources = mutableMapOf<ResourceHandle, ResourceCleanup.Reference>()
  private var projecting: Set<ResourceHandle>? = null
  private val pendingOwners = mutableMapOf<String, CoreResource>()
  private val revisions = MutableStateFlow(-1L)
  private val lifecycleErrors = MutableStateFlow<CoreException?>(null)
  public val lastLifecycleError: StateFlow<CoreException?> = lifecycleErrors.asStateFlow()
  private val teardown = mutableListOf<() -> Unit>()
  public val changes: StateFlow<Long> = revisions.asStateFlow()
  public val revision: Long get() = snapshot.revision
  public val epoch: Long get() = snapshot.epoch
  public val roots: Map<String, ResourceHandle> get() = snapshot.roots
  public val isAvailable: Boolean get() = snapshot.available
  init {
    val owner = WeakReference(this)
    transport.receive = { message -> owner.get()?.let { runtime -> runtime.scope.launch { runtime.receive(message) } } }
    RuntimeCleanup.register(this, transport, revisions)
  }
  @Synchronized public fun resource(handle: ResourceHandle): CoreResource {
    if (!isAvailable || handle !in (projecting ?: snapshot.states.keys)) throw CoreException("stale_resource")
    resources[handle]?.get()?.let { return it }
    return GeneratedBindings.makeResource(handle, this).also { resource ->
      resources[handle] = ResourceCleanup.reference(resource, this)
      snapshot.states[handle]?.let(resource.context::store)
    }
  }
  internal fun stateIfPresent(handle: ResourceHandle): Any? = snapshot.states[handle]
  public fun isInvalidated(handle: ResourceHandle): Boolean = !snapshot.available || handle !in snapshot.states
  public suspend fun initialize(publishableKey: String, callbackUrl: String, platform: String, capabilities: Set<String>): Unit = withContext(dispatcher) {
    val id = UUID.randomUUID().toString()
    val result = CompletableDeferred<JsonElement>()
    pending[id] = { outcome -> outcome.fold(result::complete, result::completeExceptionally) }
    try {
      transport.send(buildJsonObject {
        put("kind", "init"); put("id", id)
        put("configuration", buildJsonObject {
          put("locale", java.util.Locale.getDefault().toLanguageTag())
          put("publishableKey", publishableKey); put("callbackUrl", callbackUrl); put("platform", platform)
          put("protocolVersion", GeneratedBindings.protocolVersion); put("contractHash", GeneratedBindings.contractHash)
          put("capabilities", JsonArray(capabilities.map(::JsonPrimitive)))
        })
      })
      result.await()
    } catch (cancelled: CancellationException) { close(); throw cancelled }
    finally { pending.remove(id) }
    Unit
  }
  public suspend fun <T> invoke(owner: CoreResource, target: ResourceHandle, operation: String, arguments: List<JsonElement>, decode: (JsonElement) -> T): T = withContext(dispatcher) {
    if (isInvalidated(target) || target !in snapshot.states) throw CoreException("stale_resource")
    val id = UUID.randomUUID().toString()
    val result = CompletableDeferred<T>()
    pending[id] = { outcome ->
      outcome.mapCatching(decode).fold(result::complete, result::completeExceptionally)
    }
    pendingOwners[id] = owner
    try {
      transport.send(buildJsonObject { put("kind", "invoke"); put("id", id); put("target", target.toJson()); put("operation", operation); put("args", JsonArray(arguments)) })
      result.await()
    } catch (cancelled: CancellationException) {
      runCatching { transport.send(buildJsonObject { put("kind", "cancel"); put("id", id) }) }
      throw cancelled
    } finally { pending.remove(id); pendingOwners.remove(id) }
  }
  public fun checkErrorResult(value: JsonElement) {
    val error = value.jsonObject["error"] ?: throw CoreException("invalid_error_result")
    if (error != JsonNull) throw CoreException.fromJson(error, this)
  }
  private fun receive(message: JsonElement) {
    if (!snapshot.available) return
    val previousLeases = completionLeases
    try {
      val m = message.jsonObject
      completionLeases = previousLeases + (m["state"]?.let(::apply) ?: emptyList())
      when (m.getValue("kind").requireString()) {
        "ready" -> {
          val manifest = m.getValue("manifest").jsonObject
          if (manifest["contractHash"] != JsonPrimitive(GeneratedBindings.contractHash) || manifest["protocolVersion"] != JsonPrimitive(GeneratedBindings.protocolVersion)) throw CoreException("incompatible_bindings")
          pending.remove(m.getValue("id").requireString())?.invoke(Result.success(JsonNull))
        }
        "complete" -> {
          val outcome = m["failure"]?.let { Result.failure<JsonElement>(CoreException.fromJson(it, this)) } ?: Result.success(m["result"] ?: Undefined)
          pending.remove(m.getValue("id").requireString())?.invoke(outcome)
        }
        "lifecycleError" -> lifecycleErrors.value = m["failure"]?.let { CoreException.fromJson(it, this) }
        "runtimeError", "unavailable", "initializationFailed" -> fail(m["failure"]?.let { CoreException.fromJson(it, this) } ?: CoreException("runtime_unavailable"))
      }
    } catch (error: Exception) { fail(error) }
    finally { completionLeases = previousLeases }
  }
  @Synchronized private fun apply(value: JsonElement): List<CoreResource> {
    val v = value.jsonObject
    val revision = v.getValue("revision").jsonPrimitive.long
    if (revision <= snapshot.revision) return emptyList()
    val epoch = v.getValue("epoch").jsonPrimitive.long
    if (epoch < snapshot.epoch) throw CoreException("stale_state")
    val invalid = v.getValue("invalidated").jsonArray.map(ResourceHandle::fromJson)
    val projections = v.getValue("resources").jsonArray.map { it.jsonObject }
    projecting = (snapshot.states.keys + projections.map { ResourceHandle.fromJson(it.getValue("handle")) }) - invalid.toSet()
    try {
    val staged = projections.associate { p ->
      val handle = ResourceHandle.fromJson(p.getValue("handle"))
      handle to resource(handle)
    }
    val states = snapshot.states.toMutableMap()
    invalid.forEach(states::remove)
    for (p in projections) {
      val handle = ResourceHandle.fromJson(p.getValue("handle"))
      states[handle] = staged.getValue(handle).prepare(p.getValue("state"))
    }
    val roots = v.getValue("roots").jsonObject.filterValues { it != JsonNull }.mapValues { ResourceHandle.fromJson(it.value) }
    snapshot = Snapshot(revision, epoch, roots, states.toMap())
    staged.forEach { (handle, resource) -> states[handle]?.let(resource.context::store) }
    invalid.forEach(resources::remove)
    revisions.value = revision
    return staged.values.toList()
    } finally { projecting = null }
  }
  internal fun release(reference: ResourceCleanup.Reference) {
    scope.launch {
      val handle = reference.handle
      if (!isAvailable || handle in snapshot.roots.values || resources[handle] !== reference || reference.get() != null) return@launch
      resources.remove(handle)
      snapshot = snapshot.copy(states = snapshot.states - handle)
      runCatching { transport.send(buildJsonObject { put("kind", "release"); put("target", handle.toJson()) }) }
    }
  }
  private fun fail(error: Exception) {
    snapshot = snapshot.copy(available = false)
    val calls = pending.values.toList()
    pending.clear()
    pendingOwners.clear()
    calls.forEach { it(Result.failure(error)) }
    revisions.value += 1
  }
  public fun setApplicationActive(active: Boolean) {
    scope.launch {
      if (!isAvailable) return@launch
      runCatching { transport.send(buildJsonObject { put("kind", "lifecycle"); put("state", if (active) "foreground" else "background") }) }
        .onFailure { fail(it as? Exception ?: CoreException("runtime_unavailable")) }
    }
  }
  internal fun setNetworkOnline(online: Boolean) {
    scope.launch {
      if (!isAvailable) return@launch
      runCatching { transport.send(buildJsonObject { put("kind", "connectivity"); put("online", online) }) }
        .onFailure { fail(it as? Exception ?: CoreException("runtime_unavailable")) }
    }
  }
  internal fun addTeardown(action: () -> Unit) { teardown += action }
  override fun close() {
    scope.launch {
      teardown.forEach { it() }; teardown.clear()
      runCatching { transport.send(buildJsonObject { put("kind", "dispose") }) }
      transport.close()
      fail(CoreException("runtime_disposed"))
      snapshot = snapshot.copy(states = emptyMap(), roots = emptyMap())
      resources.clear()
      scope.cancel()
    }
  }
}
