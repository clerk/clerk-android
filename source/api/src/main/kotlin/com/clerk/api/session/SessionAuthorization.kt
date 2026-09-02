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
  fun evaluate(session: Session, params: CheckAuthorizationParams): Boolean {
    val membership =
      session.user?.organizationMemberships?.firstOrNull {
        it.organization.id == session.lastActiveOrganizationId
      }
    val jwt = session.lastActiveToken?.jwt
    return evaluate(
      AuthorizationContext(
        userId = session.user?.id,
        org =
          OrgAuthorizationContext(
            orgId = membership?.organization?.id,
            orgRole = membership?.role,
            orgPermissions = membership?.permissions,
          ),
        billing =
          BillingAuthorizationContext(
            features = jwt?.let { jwtManager.featuresClaim(it) }.orEmpty(),
            plans = jwt?.let { jwtManager.plansClaim(it) }.orEmpty(),
          ),
        factorVerificationAge = session.factorVerificationAge,
      ),
      params,
    )
  }

  fun evaluate(context: AuthorizationContext, params: CheckAuthorizationParams): Boolean {
    if (context.userId.isNullOrEmpty()) {
      return false
    }
    val results =
      listOf(
        checkOrgAuthorization(context.org, params),
        checkBillingAuthorization(context.billing, params),
        checkReverificationAuthorization(context.factorVerificationAge, params.reverification),
      )
    return results.any { it == CheckResult.PASS } &&
      results.all { it == CheckResult.PASS || it == CheckResult.SKIP }
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
      require(colonIndex != -1) { "Invalid claim element (missing colon): $trimmed" }
      val value = trimmed.substring(colonIndex + 1)
      when (trimmed.substring(0, colonIndex)) {
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
}

internal data class AuthorizationContext(
  val userId: String?,
  val org: OrgAuthorizationContext,
  val billing: BillingAuthorizationContext,
  val factorVerificationAge: List<Int>?,
)

internal data class OrgAuthorizationContext(
  val orgId: String?,
  val orgRole: String?,
  val orgPermissions: List<String>?,
)

internal data class BillingAuthorizationContext(val features: String?, val plans: String?)

private enum class CheckResult {
  PASS,
  FAIL,
  SKIP,
}

private const val FACTOR_NOT_ENROLLED = -1
private const val STRICT_AFTER_MINUTES = 10
private const val MODERATE_AFTER_MINUTES = 60
private const val LAX_AFTER_MINUTES = 1_440
private val orgScopes = setOf("o", "org", "organization")
private val userScopes = setOf("u", "user")
private val allowedLevels = setOf("first_factor", "second_factor", "multi_factor")
private val orgPrefix = Regex("^(org:)*")
private val jwtManager: JWTManager = JWTManagerImpl()

private fun checkOrgAuthorization(
  org: OrgAuthorizationContext,
  params: CheckAuthorizationParams,
): CheckResult {
  val role = params.role
  val permission = params.permission
  val roleMatches =
    role == null ||
      (!org.orgRole.isNullOrEmpty() && prefixWithOrg(org.orgRole) == prefixWithOrg(role))
  val permissionMatches =
    permission == null ||
      (org.orgPermissions != null && org.orgPermissions.contains(prefixWithOrg(permission)))
  return when {
    role == null && permission == null -> CheckResult.SKIP
    org.orgId.isNullOrEmpty() || !roleMatches || !permissionMatches -> CheckResult.FAIL
    else -> CheckResult.PASS
  }
}

private fun prefixWithOrg(value: String): String {
  return "org:" + value.replace(orgPrefix, "")
}

private fun checkBillingAuthorization(
  billing: BillingAuthorizationContext,
  params: CheckAuthorizationParams,
): CheckResult {
  val feature = params.feature
  val plan = params.plan
  val features = billing.features
  val plans = billing.plans
  val featureMatches =
    when {
      feature == null -> true
      features.isNullOrEmpty() -> false
      else ->
        try {
          checkForFeatureOrPlan(features, feature)
        } catch (_: IllegalArgumentException) {
          false
        }
    }
  val planMatches =
    when {
      plan == null -> true
      plans.isNullOrEmpty() -> false
      else ->
        try {
          checkForFeatureOrPlan(plans, plan)
        } catch (_: IllegalArgumentException) {
          false
        }
    }
  return when {
    feature == null && plan == null -> CheckResult.SKIP
    featureMatches && planMatches -> CheckResult.PASS
    else -> CheckResult.FAIL
  }
}

private fun checkForFeatureOrPlan(claim: String, featureOrPlan: String): Boolean {
  val (orgFeatures, userFeatures) = SessionAuthorization.splitByScope(claim)
  val parts = featureOrPlan.split(":")
  val rawScope = parts[0]
  val hasExplicitScope = parts.size > 1
  val id = if (hasExplicitScope) parts[1] else rawScope
  require(!hasExplicitScope || rawScope in orgScopes || rawScope in userScopes) {
    "Invalid scope: $rawScope"
  }
  return when {
    hasExplicitScope && rawScope in orgScopes -> orgFeatures.contains(id)
    hasExplicitScope && rawScope in userScopes -> userFeatures.contains(id)
    else -> (orgFeatures + userFeatures).contains(id)
  }
}

private fun checkReverificationAuthorization(
  factorVerificationAge: List<Int>?,
  reverification: ReverificationConfig?,
): CheckResult {
  val resolved = reverification?.let(::resolveReverification)
  return when {
    reverification == null -> CheckResult.SKIP
    resolved == null -> CheckResult.FAIL
    factorVerificationAge == null -> CheckResult.FAIL
    factorVerificationAge.size != 2 -> CheckResult.FAIL
    else -> factorFreshness(factorVerificationAge[0], factorVerificationAge[1], resolved)
  }
}

private fun factorFreshness(
  factor1Age: Int,
  factor2Age: Int,
  resolved: Pair<SessionVerification.Level, Int>,
): CheckResult {
  val afterMinutes = resolved.second
  val age1Ok = factor1Age == FACTOR_NOT_ENROLLED || factor1Age >= 0
  val age2Ok = factor2Age == FACTOR_NOT_ENROLLED || factor2Age >= 0
  val factor1Fresh = factor1Age != FACTOR_NOT_ENROLLED && afterMinutes > factor1Age
  val factor2Fresh = factor2Age != FACTOR_NOT_ENROLLED && afterMinutes > factor2Age
  return when {
    !age1Ok || !age2Ok -> CheckResult.FAIL
    factor1Age == FACTOR_NOT_ENROLLED && factor2Age == FACTOR_NOT_ENROLLED -> CheckResult.FAIL
    else ->
      authorizedForLevel(
        resolved.first,
        factor1Age,
        factor2Age,
        factor1Fresh,
        factor2Fresh,
      )
  }
}

private fun authorizedForLevel(
  level: SessionVerification.Level,
  factor1Age: Int,
  factor2Age: Int,
  factor1Fresh: Boolean,
  factor2Fresh: Boolean,
): CheckResult {
  val secondOk = if (factor2Age == FACTOR_NOT_ENROLLED) factor1Fresh else factor2Fresh
  val multiOk =
    if (factor2Age == FACTOR_NOT_ENROLLED) {
      factor1Fresh
    } else {
      factor1Age != FACTOR_NOT_ENROLLED && factor1Fresh && factor2Fresh
    }
  return when (level) {
    SessionVerification.Level.FIRST_FACTOR ->
      if (factor1Fresh) CheckResult.PASS else CheckResult.FAIL
    SessionVerification.Level.SECOND_FACTOR -> if (secondOk) CheckResult.PASS else CheckResult.FAIL
    SessionVerification.Level.MULTI_FACTOR -> if (multiOk) CheckResult.PASS else CheckResult.FAIL
    SessionVerification.Level.UNKNOWN -> CheckResult.FAIL
  }
}

private fun resolveReverification(
  config: ReverificationConfig
): Pair<SessionVerification.Level, Int>? {
  return when (config) {
    ReverificationConfig.StrictMfa -> SessionVerification.Level.MULTI_FACTOR to STRICT_AFTER_MINUTES
    ReverificationConfig.Strict -> SessionVerification.Level.SECOND_FACTOR to STRICT_AFTER_MINUTES
    ReverificationConfig.Moderate ->
      SessionVerification.Level.SECOND_FACTOR to MODERATE_AFTER_MINUTES
    ReverificationConfig.Lax -> SessionVerification.Level.SECOND_FACTOR to LAX_AFTER_MINUTES
    is ReverificationConfig.Custom ->
      config
        .takeIf { it.level.value in allowedLevels && it.afterMinutes > 0 }
        ?.let {
          it.level to it.afterMinutes
        }
  }
}
