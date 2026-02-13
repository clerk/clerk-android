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
        currentVersion = normalizedCurrentVersion,
        minimumVersion = null,
        updateUrl = null,
        reason = ForceUpdateStatus.Reason.NO_POLICY,
      )
    }

    val minimumVersion = policy.minimumVersion.trim().ifEmpty { null }
    val updateUrl = policy.updateUrl?.trim()?.takeIf { it.isNotEmpty() }

    if (minimumVersion == null) {
      return ForceUpdateStatus(
        isSupported = true,
        currentVersion = normalizedCurrentVersion,
        minimumVersion = null,
        updateUrl = updateUrl,
        reason = ForceUpdateStatus.Reason.NO_POLICY,
      )
    }

    if (normalizedCurrentVersion == null) {
      return ForceUpdateStatus(
        isSupported = true,
        currentVersion = null,
        minimumVersion = minimumVersion,
        updateUrl = updateUrl,
        reason = ForceUpdateStatus.Reason.MISSING_CURRENT_VERSION,
      )
    }

    if (!AppVersionComparator.isValid(normalizedCurrentVersion)) {
      return ForceUpdateStatus(
        isSupported = true,
        currentVersion = normalizedCurrentVersion,
        minimumVersion = minimumVersion,
        updateUrl = updateUrl,
        reason = ForceUpdateStatus.Reason.INVALID_CURRENT_VERSION,
      )
    }

    if (!AppVersionComparator.isValid(minimumVersion)) {
      return ForceUpdateStatus(
        isSupported = true,
        currentVersion = normalizedCurrentVersion,
        minimumVersion = minimumVersion,
        updateUrl = updateUrl,
        reason = ForceUpdateStatus.Reason.INVALID_MINIMUM_VERSION,
      )
    }

    val isSupported =
      AppVersionComparator.isSupported(
        current = normalizedCurrentVersion,
        minimum = minimumVersion,
      ) ?: true

    return ForceUpdateStatus(
      isSupported = isSupported,
      currentVersion = normalizedCurrentVersion,
      minimumVersion = minimumVersion,
      updateUrl = updateUrl,
      reason =
        if (isSupported) {
          ForceUpdateStatus.Reason.SUPPORTED
        } else {
          ForceUpdateStatus.Reason.BELOW_MINIMUM
        },
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
      currentVersion = meta["current_version"]?.jsonPrimitive?.contentOrNull,
      minimumVersion = meta["minimum_version"]?.jsonPrimitive?.contentOrNull,
      updateUrl = meta["update_url"]?.jsonPrimitive?.contentOrNull,
      reason = ForceUpdateStatus.Reason.SERVER_REJECTED,
    )
  }
}
