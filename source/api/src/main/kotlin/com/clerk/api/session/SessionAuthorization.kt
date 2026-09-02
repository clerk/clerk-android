package com.clerk.api.session

/**
 * Parameters for [Session.checkAuthorization] and [Session.has].
 *
 * Matches clerk-js `CheckAuthorizationParams`. The public TypeScript type treats role, permission,
 * feature, and plan as mutually exclusive. The runtime combiner still ANDs every dimension that is
 * present, including feature + plan.
 */
data class CheckAuthorizationParams(
  val role: String? = null,
  val permission: String? = null,
  val feature: String? = null,
  val plan: String? = null,
  val reverification: ReverificationConfig? = null,
)

/**
 * Reverification requirement for [Session.checkAuthorization].
 *
 * Matches clerk-js `ReverificationConfig`: presets `strict_mfa`, `strict`, `moderate`, `lax`, or a
 * custom `{ level, afterMinutes }` object.
 */
sealed class ReverificationConfig {
  data object StrictMfa : ReverificationConfig()

  data object Strict : ReverificationConfig()

  data object Moderate : ReverificationConfig()

  data object Lax : ReverificationConfig()

  data class Custom(val level: SessionVerification.Level, val afterMinutes: Int) :
    ReverificationConfig()
}

internal object SessionAuthorization {
  private val jwtManager: JWTManager = JWTManagerImpl()

  private val orgScopes = setOf("o", "org", "organization")
  private val userScopes = setOf("u", "user")
  private val allowedLevels = setOf("first_factor", "second_factor", "multi_factor")

  private enum class CheckResult {
    PASS,
    FAIL,
    SKIP,
  }

  fun evaluate(
    session: Session,
    role: String?,
    permission: String?,
    feature: String?,
    plan: String?,
    reverification: ReverificationConfig?,
  ): Boolean {
    val membership =
      session.user?.organizationMemberships?.firstOrNull {
        it.organization.id == session.lastActiveOrganizationId
      }
    val jwt = session.lastActiveToken?.jwt
    return evaluate(
      userId = session.user?.id,
      orgId = membership?.organization?.id,
      orgRole = membership?.role,
      orgPermissions = membership?.permissions,
      factorVerificationAge = session.factorVerificationAge,
      features = jwt?.let { jwtManager.featuresClaim(it) }.orEmpty(),
      plans = jwt?.let { jwtManager.plansClaim(it) }.orEmpty(),
      role = role,
      permission = permission,
      feature = feature,
      plan = plan,
      reverification = reverification,
    )
  }

  fun evaluate(
    userId: String?,
    orgId: String?,
    orgRole: String?,
    orgPermissions: List<String>?,
    factorVerificationAge: List<Int>?,
    features: String?,
    plans: String?,
    role: String?,
    permission: String?,
    feature: String?,
    plan: String?,
    reverification: ReverificationConfig?,
  ): Boolean {
    if (userId.isNullOrEmpty()) {
      return false
    }

    return combine(
      listOf(
        checkOrgAuthorization(
          role = role,
          permission = permission,
          orgId = orgId,
          orgRole = orgRole,
          orgPermissions = orgPermissions,
        ),
        checkBillingAuthorization(
          feature = feature,
          plan = plan,
          features = features,
          plans = plans,
        ),
        checkReverificationAuthorization(
          reverification = reverification,
          factorVerificationAge = factorVerificationAge,
        ),
      )
    )
  }

  fun splitByScope(claim: String?): Pair<List<String>, List<String>> {
    val org = mutableListOf<String>()
    val user = mutableListOf<String>()

    if (claim.isNullOrEmpty()) {
      return org to user
    }

    for (part in claim.split(",")) {
      val trimmed = part.trim()
      val colonIndex = trimmed.indexOf(':')
      if (colonIndex == -1) {
        throw IllegalArgumentException("Invalid claim element (missing colon): $trimmed")
      }
      val scope = trimmed.substring(0, colonIndex)
      val value = trimmed.substring(colonIndex + 1)
      when (scope) {
        "o" -> org.add(value)
        "u" -> user.add(value)
        "ou",
        "uo" -> {
          org.add(value)
          user.add(value)
        }
      }
    }

    return org to user
  }

  private fun combine(results: List<CheckResult>): Boolean {
    return results.any { it == CheckResult.PASS } &&
      results.all { it == CheckResult.PASS || it == CheckResult.SKIP }
  }

  private fun prefixWithOrg(value: String): String {
    return "org:" + value.replace(Regex("^(org:)*"), "")
  }

  private fun checkOrgAuthorization(
    role: String?,
    permission: String?,
    orgId: String?,
    orgRole: String?,
    orgPermissions: List<String>?,
  ): CheckResult {
    val roleAsked = role != null
    val permissionAsked = permission != null

    if (!roleAsked && !permissionAsked) {
      return CheckResult.SKIP
    }

    if (orgId.isNullOrEmpty()) {
      return CheckResult.FAIL
    }

    if (roleAsked) {
      if (orgRole.isNullOrEmpty() || prefixWithOrg(orgRole) != prefixWithOrg(role as String)) {
        return CheckResult.FAIL
      }
    }

    if (permissionAsked) {
      if (orgPermissions == null || !orgPermissions.contains(prefixWithOrg(permission as String))) {
        return CheckResult.FAIL
      }
    }

    return CheckResult.PASS
  }

  private fun checkBillingAuthorization(
    feature: String?,
    plan: String?,
    features: String?,
    plans: String?,
  ): CheckResult {
    val featureAsked = feature != null
    val planAsked = plan != null

    if (!featureAsked && !planAsked) {
      return CheckResult.SKIP
    }

    if (featureAsked) {
      if (features.isNullOrEmpty()) {
        return CheckResult.FAIL
      }
      try {
        if (!checkForFeatureOrPlan(features, feature as String)) {
          return CheckResult.FAIL
        }
      } catch (_: Exception) {
        return CheckResult.FAIL
      }
    }

    if (planAsked) {
      if (plans.isNullOrEmpty()) {
        return CheckResult.FAIL
      }
      try {
        if (!checkForFeatureOrPlan(plans, plan as String)) {
          return CheckResult.FAIL
        }
      } catch (_: Exception) {
        return CheckResult.FAIL
      }
    }

    return CheckResult.PASS
  }

  private fun checkForFeatureOrPlan(claim: String, featureOrPlan: String): Boolean {
    val (orgFeatures, userFeatures) = splitByScope(claim)
    val parts = featureOrPlan.split(":")
    val rawScope = parts[0]
    val hasExplicitScope = parts.size > 1
    val id = if (hasExplicitScope) parts[1] else rawScope

    if (hasExplicitScope && rawScope !in orgScopes && rawScope !in userScopes) {
      throw IllegalArgumentException("Invalid scope: $rawScope")
    }

    if (hasExplicitScope) {
      if (rawScope in orgScopes) {
        return orgFeatures.contains(id)
      }
      if (rawScope in userScopes) {
        return userFeatures.contains(id)
      }
    }

    return (orgFeatures + userFeatures).contains(id)
  }

  private fun checkReverificationAuthorization(
    reverification: ReverificationConfig?,
    factorVerificationAge: List<Int>?,
  ): CheckResult {
    if (reverification == null) {
      return CheckResult.SKIP
    }

    if (factorVerificationAge == null) {
      return CheckResult.FAIL
    }

    if (
      factorVerificationAge.size != 2 ||
        !isValidFactorAge(factorVerificationAge[0]) ||
        !isValidFactorAge(factorVerificationAge[1])
    ) {
      return CheckResult.FAIL
    }

    val resolved = resolveReverification(reverification) ?: return CheckResult.FAIL
    val factor1Age = factorVerificationAge[0]
    val factor2Age = factorVerificationAge[1]
    val afterMinutes = resolved.second

    if (factor1Age == -1 && factor2Age == -1) {
      return CheckResult.FAIL
    }

    val factor1FreshEnough = factor1Age != -1 && afterMinutes > factor1Age
    val factor2FreshEnough = factor2Age != -1 && afterMinutes > factor2Age

    return when (resolved.first) {
      SessionVerification.Level.FIRST_FACTOR ->
        if (factor1FreshEnough) CheckResult.PASS else CheckResult.FAIL
      SessionVerification.Level.SECOND_FACTOR -> {
        if (factor2Age == -1) {
          if (factor1FreshEnough) CheckResult.PASS else CheckResult.FAIL
        } else if (factor1Age == -1) {
          if (factor2FreshEnough) CheckResult.PASS else CheckResult.FAIL
        } else {
          if (factor2FreshEnough) CheckResult.PASS else CheckResult.FAIL
        }
      }
      SessionVerification.Level.MULTI_FACTOR -> {
        if (factor2Age == -1) {
          if (factor1FreshEnough) CheckResult.PASS else CheckResult.FAIL
        } else if (factor1Age == -1) {
          CheckResult.FAIL
        } else {
          if (factor1FreshEnough && factor2FreshEnough) CheckResult.PASS else CheckResult.FAIL
        }
      }
      SessionVerification.Level.UNKNOWN -> CheckResult.FAIL
    }
  }

  private fun resolveReverification(
    config: ReverificationConfig
  ): Pair<SessionVerification.Level, Int>? {
    return when (config) {
      ReverificationConfig.StrictMfa -> SessionVerification.Level.MULTI_FACTOR to 10
      ReverificationConfig.Strict -> SessionVerification.Level.SECOND_FACTOR to 10
      ReverificationConfig.Moderate -> SessionVerification.Level.SECOND_FACTOR to 60
      ReverificationConfig.Lax -> SessionVerification.Level.SECOND_FACTOR to 1440
      is ReverificationConfig.Custom -> {
        if (config.level.value !in allowedLevels || config.afterMinutes <= 0) {
          null
        } else {
          config.level to config.afterMinutes
        }
      }
    }
  }

  private fun isValidFactorAge(value: Int): Boolean {
    return value == -1 || value >= 0
  }
}
