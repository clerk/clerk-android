package com.clerk.ui.core.preview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.clerk.api.*
import com.clerk.ui.core.composition.ClerkProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.*

private val LocalPreviewFixture = staticCompositionLocalOf<ClerkPreviewFixture?> { null }

@Composable
internal fun previewResource(type: String, id: String? = null): CoreResource =
  checkNotNull(LocalPreviewFixture.current) { "Use previewResource inside ClerkPreview." }
    .resource(type, id)

/** Real generated projections, produced by the same TypeScript serializer as the bundled core. */
internal class ClerkPreviewFixture(context: Context, variant: String = "default") : AutoCloseable {
  private val document =
    context.assets.open("clerk-preview/$variant.json").bufferedReader().use {
      Json.parseToJsonElement(it.readText()).jsonObject
    }
  private val transport =
    object : CoreTransport {
      override var receive: ((JsonElement) -> Unit)? = null

      override fun send(message: JsonElement) {
        val request = message.jsonObject
        if (request["kind"] != JsonPrimitive("invoke")) return
        receive?.invoke(
          buildJsonObject {
            put("kind", "complete")
            put("id", request.getValue("id"))
            val result =
              document
                .getValue("results")
                .jsonObject[request.getValue("operation").jsonPrimitive.content]
            if (result != null) put("result", result)
            else
              put(
                "failure",
                buildJsonObject {
                  put("kind", "bridge")
                  put("code", "preview_read_only")
                  put("message", "This operation is unavailable in the preview fixture.")
                },
              )
          }
        )
      }

      override fun close() {
        receive = null
      }
    }
  // Fixture delivery is synchronous, in-process, and never executes networking or JS.
  private val runtime = CoreRuntime(transport, Dispatchers.Unconfined)
  val clerk: Clerk

  init {
    transport.receive?.invoke(
      buildJsonObject {
        put("kind", "ready")
        put("id", "preview")
        put("manifest", document.getValue("manifest"))
        put("state", document.getValue("state"))
      }
    )
    clerk = runtime.resource(runtime.roots.getValue("clerk")) as Clerk
  }

  fun resource(type: String, id: String? = null): CoreResource {
    val record =
      document
        .getValue("state")
        .jsonObject
        .getValue("resources")
        .jsonArray
        .first {
          val item = it.jsonObject
          item.getValue("handle").jsonObject["type"] == JsonPrimitive(type) &&
            (id == null || item.getValue("state").jsonObject["id"] == JsonPrimitive(id))
        }
        .jsonObject
    return runtime.resource(ResourceHandle.fromJson(record.getValue("handle")))
  }

  override fun close() = runtime.close()
}

@Composable
internal fun ClerkPreview(variant: String = "default", content: @Composable (Clerk) -> Unit) {
  val context = LocalContext.current
  val fixture = remember(context, variant) { ClerkPreviewFixture(context, variant) }
  DisposableEffect(fixture) { onDispose { fixture.close() } }
  CompositionLocalProvider(LocalPreviewFixture provides fixture) {
    ClerkProvider(fixture.clerk) { content(fixture.clerk) }
  }
}
