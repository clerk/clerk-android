package com.clerk.api.appversion

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.clerk.api.log.ClerkLogger
import com.clerk.api.network.model.environment.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal data class AppBuild(val packageName: String, val versionCode: Long) {
  companion object {
    fun from(context: Context): AppBuild? =
      try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        if (versionCode < 1) {
          ClerkLogger.w("This app's versionCode is $versionCode, so Clerk won't check its version.")
          null
        } else {
          AppBuild(context.packageName, versionCode)
        }
      } catch (e: PackageManager.NameNotFoundException) {
        ClerkLogger.w("Couldn't read this app's versionCode: ${e.message}")
        null
      }
  }
}

internal object AppVersionSupport {
  private val lock = Any()
  private var environmentStatus: AppVersionSupportStatus? = null
  private var serverStatus: AppVersionSupportStatus? = null

  @Volatile
  var build: AppBuild? = null
    private set

  @Volatile
  var generation: Long = 0
    private set

  private val _status = MutableStateFlow(AppVersionSupportStatus.supported(currentVersion = null))
  val status: StateFlow<AppVersionSupportStatus> = _status.asStateFlow()

  private val _resets = MutableStateFlow(0L)
  val resets: StateFlow<Long> = _resets.asStateFlow()

  fun configure(build: AppBuild?) =
    synchronized(lock) {
      this.build = build
      publishLocked()
    }

  fun applySavedEnvironment(environment: Environment) =
    synchronized(lock) {
      environmentStatus = resolve(environment)
      publishLocked()
    }

  fun applyFetchedEnvironment(environment: Environment) =
    synchronized(lock) {
      serverStatus = null
      environmentStatus = resolve(environment)
      publishLocked()
    }

  fun applyUnsupportedAppVersion(meta: JsonObject?, generation: Long) =
    synchronized(lock) {
      if (generation != this.generation) return@synchronized
      serverStatus =
        AppVersionSupportStatus(
          isSupported = false,
          currentVersion = meta.string("current_version")?.toVersionCode() ?: build?.versionCode,
          minimumVersion =
            meta.string("minimum_version")?.toVersionCode() ?: environmentStatus?.minimumVersion,
          updateUrl =
            meta.string("update_url")?.takeIf(::isStoreUrl) ?: environmentStatus?.updateUrl,
        )
      publishLocked()
    }

  fun applySupportedAppVersion(generation: Long) =
    synchronized(lock) {
      if (generation != this.generation) return@synchronized
      serverStatus =
        environmentStatus?.takeIf { it.isSupported }
          ?: AppVersionSupportStatus.supported(build?.versionCode)
      publishLocked()
    }

  fun reset() =
    synchronized(lock) {
      generation += 1
      _resets.value += 1
      build = null
      environmentStatus = null
      serverStatus = null
      publishLocked()
    }

  private fun resolve(environment: Environment): AppVersionSupportStatus {
    val build = build
    val policy = build?.let { build ->
      environment.nativeAppSettings.minimumSupportedVersion.android.firstOrNull {
        it.packageName == build.packageName
      }
    }
    if (build == null || policy == null)
      return AppVersionSupportStatus.supported(build?.versionCode)
    val minimumVersion = policy.minimumVersion.toVersionCode()
    return AppVersionSupportStatus(
      isSupported = minimumVersion == null || build.versionCode >= minimumVersion,
      currentVersion = build.versionCode,
      minimumVersion = minimumVersion,
      updateUrl = policy.updateUrl?.takeIf(::isStoreUrl),
    )
  }

  private fun publishLocked() {
    _status.value =
      serverStatus ?: environmentStatus ?: AppVersionSupportStatus.supported(build?.versionCode)
  }
}

private val versionCodePattern = Regex("^[1-9]\\d*$")

internal fun String.toVersionCode(): Long? = takeIf(versionCodePattern::matches)?.toLongOrNull()

private fun isStoreUrl(value: String): Boolean {
  val uri = value.toUri()
  return uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrEmpty()
}

private fun JsonObject?.string(key: String): String? =
  (this?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
