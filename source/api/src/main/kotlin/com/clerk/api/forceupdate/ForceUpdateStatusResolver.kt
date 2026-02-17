package com.clerk.api.forceupdate

import com.clerk.api.network.model.environment.Environment
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object ForceUpdateStatusResolver {
  fun fromEnvironment(
    environment: Environment?,
    packageName: String?,
    currentVersion: String?,
  ): ForceUpdateStatus {
    val normalizedCurrentVersion = currentVersion?.trim()?.takeIf { it.isNotEmpty() }
    val normalizedPackageName = packageName?.trim()?.takeIf { it.isNotEmpty() }

    val policy =
      environment
        ?.forceUpdate
        ?.androidPolicies
        ?.firstOrNull { it.packageName == normalizedPackageName }

    if (policy == null) {
      return ForceUpdateStatus(
        isSupported = true,
        minimumVersion = null,
        updateUrl = null,
      )
    }

    val minimumVersion = policy.minimumVersion.trim().ifEmpty { null }
    val updateUrl = policy.updateUrl?.trim()?.takeIf { it.isNotEmpty() }

    if (minimumVersion == null) {
      return ForceUpdateStatus(
        isSupported = true,
        minimumVersion = null,
        updateUrl = updateUrl,
      )
    }

    if (normalizedCurrentVersion == null) {
      return ForceUpdateStatus(
        isSupported = true,
        minimumVersion = minimumVersion,
        updateUrl = updateUrl,
      )
    }

    if (!AppVersionComparator.isValid(normalizedCurrentVersion)) {
      return ForceUpdateStatus(
        isSupported = true,
        minimumVersion = minimumVersion,
        updateUrl = updateUrl,
      )
    }

    if (!AppVersionComparator.isValid(minimumVersion)) {
      return ForceUpdateStatus(
        isSupported = true,
        minimumVersion = minimumVersion,
        updateUrl = updateUrl,
      )
    }

    val isSupported =
      AppVersionComparator.isSupported(
        current = normalizedCurrentVersion,
        minimum = minimumVersion,
      ) ?: true

    return ForceUpdateStatus(
      isSupported = isSupported,
      minimumVersion = minimumVersion,
      updateUrl = updateUrl,
    )
  }

  fun fromUnsupportedVersionMeta(meta: JsonObject?, packageName: String?): ForceUpdateStatus? {
    if (meta == null) {
      return null
    }

    val platform = meta["platform"]?.jsonPrimitive?.contentOrNull?.lowercase()
    if (platform != null && platform != "android") {
      return null
    }

    val currentPackageName = packageName?.trim()?.takeIf { it.isNotEmpty() }
    val responsePackageName =
      meta["app_identifier"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
    if (
      currentPackageName != null &&
        responsePackageName != null &&
        responsePackageName != currentPackageName
    ) {
      return null
    }

    return ForceUpdateStatus(
      isSupported = false,
      minimumVersion = meta["minimum_version"]?.jsonPrimitive?.contentOrNull,
      updateUrl = meta["update_url"]?.jsonPrimitive?.contentOrNull,
    )
  }
}
