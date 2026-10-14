package com.clerk.api.network.serialization

import com.clerk.api.billing.BillingDiscountEffect
import com.clerk.api.billing.BillingDiscountRedemptionStatus
import com.clerk.api.billing.BillingDiscountSource
import com.clerk.api.billing.BillingPayerResourceType
import com.clerk.api.billing.BillingPaymentChargeType
import com.clerk.api.billing.BillingPaymentMethodStatus
import com.clerk.api.billing.BillingPaymentStatus
import com.clerk.api.billing.BillingStatementStatus
import com.clerk.api.billing.BillingSubscriptionPlanPeriod
import com.clerk.api.billing.BillingSubscriptionStatus
import com.clerk.api.biometriccredential.BiometricCredential
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.environment.InstanceEnvironmentType
import com.clerk.api.network.model.environment.PreferredSignInStrategy
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class UnknownFallbackEnumSerializerTest {

  private class ServerEnum<E : Enum<E>>(
    val serializer: KSerializer<E>,
    val fallback: E,
    val wireNames: Map<E, String>,
  )

  private val serverEnumsWithWireNames: List<ServerEnum<*>> =
    listOf(
      ServerEnum(
        SignIn.Status.serializer(),
        SignIn.Status.UNKNOWN,
        mapOf(
          SignIn.Status.COMPLETE to "complete",
          SignIn.Status.NEEDS_FIRST_FACTOR to "needs_first_factor",
          SignIn.Status.NEEDS_SECOND_FACTOR to "needs_second_factor",
          SignIn.Status.NEEDS_IDENTIFIER to "needs_identifier",
          SignIn.Status.NEEDS_NEW_PASSWORD to "needs_new_password",
          SignIn.Status.NEEDS_CLIENT_TRUST to "needs_client_trust",
          SignIn.Status.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        SignUp.Status.serializer(),
        SignUp.Status.UNKNOWN,
        mapOf(
          SignUp.Status.ABANDONED to "abandoned",
          SignUp.Status.MISSING_REQUIREMENTS to "missing_requirements",
          SignUp.Status.COMPLETE to "complete",
          SignUp.Status.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        Verification.Status.serializer(),
        Verification.Status.UNKNOWN,
        mapOf(
          Verification.Status.UNVERIFIED to "unverified",
          Verification.Status.VERIFIED to "verified",
          Verification.Status.TRANSFERABLE to "transferable",
          Verification.Status.FAILED to "failed",
          Verification.Status.EXPIRED to "expired",
          Verification.Status.UNKNOWN to "state_unknown",
        ),
      ),
      ServerEnum(
        InstanceEnvironmentType.serializer(),
        InstanceEnvironmentType.UNKNOWN,
        mapOf(
          InstanceEnvironmentType.PRODUCTION to "production",
          InstanceEnvironmentType.DEVELOPMENT to "development",
          InstanceEnvironmentType.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        PreferredSignInStrategy.serializer(),
        PreferredSignInStrategy.UNKNOWN,
        mapOf(
          PreferredSignInStrategy.PASSWORD to "password",
          PreferredSignInStrategy.OTP to "otp",
          PreferredSignInStrategy.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        OrganizationInvitation.Status.serializer(),
        OrganizationInvitation.Status.Unknown,
        mapOf(
          OrganizationInvitation.Status.Pending to "pending",
          OrganizationInvitation.Status.Accepted to "accepted",
          OrganizationInvitation.Status.Revoked to "revoked",
          OrganizationInvitation.Status.Invalid to "invalid",
          OrganizationInvitation.Status.Completed to "completed",
          OrganizationInvitation.Status.Unknown to "unknown",
        ),
      ),
      ServerEnum(
        BiometricCredential.Platform.serializer(),
        BiometricCredential.Platform.UNKNOWN,
        mapOf(
          BiometricCredential.Platform.IOS to "ios",
          BiometricCredential.Platform.ANDROID to "android",
          BiometricCredential.Platform.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BiometricCredential.Status.serializer(),
        BiometricCredential.Status.UNKNOWN,
        mapOf(
          BiometricCredential.Status.ACTIVE to "active",
          BiometricCredential.Status.REVOKED to "revoked",
          BiometricCredential.Status.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        Session.SessionStatus.serializer(),
        Session.SessionStatus.UNKNOWN,
        mapOf(
          Session.SessionStatus.ABANDONED to "abandoned",
          Session.SessionStatus.ACTIVE to "active",
          Session.SessionStatus.ENDED to "ended",
          Session.SessionStatus.EXPIRED to "expired",
          Session.SessionStatus.REMOVED to "removed",
          Session.SessionStatus.REPLACED to "replaced",
          Session.SessionStatus.REVOKED to "revoked",
          Session.SessionStatus.UNKNOWN to "unknown",
          Session.SessionStatus.PENDING to "pending",
        ),
      ),
      ServerEnum(
        SessionVerification.Status.serializer(),
        SessionVerification.Status.UNKNOWN,
        mapOf(
          SessionVerification.Status.NEEDS_FIRST_FACTOR to "needs_first_factor",
          SessionVerification.Status.NEEDS_SECOND_FACTOR to "needs_second_factor",
          SessionVerification.Status.COMPLETE to "complete",
          SessionVerification.Status.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        SessionVerification.Level.serializer(),
        SessionVerification.Level.UNKNOWN,
        mapOf(
          SessionVerification.Level.FIRST_FACTOR to "first_factor",
          SessionVerification.Level.SECOND_FACTOR to "second_factor",
          SessionVerification.Level.MULTI_FACTOR to "multi_factor",
          SessionVerification.Level.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BillingPayerResourceType.serializer(),
        BillingPayerResourceType.UNKNOWN,
        mapOf(
          BillingPayerResourceType.ORG to "org",
          BillingPayerResourceType.USER to "user",
          BillingPayerResourceType.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BillingDiscountEffect.serializer(),
        BillingDiscountEffect.UNKNOWN,
        mapOf(
          BillingDiscountEffect.PERCENTAGE to "percentage",
          BillingDiscountEffect.FIXED_AMOUNT to "fixed_amount",
          BillingDiscountEffect.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BillingDiscountSource.serializer(),
        BillingDiscountSource.UNKNOWN,
        mapOf(
          BillingDiscountSource.PROMOTION to "promotion",
          BillingDiscountSource.MANUAL to "manual",
          BillingDiscountSource.PROMO_CODE to "promo_code",
          BillingDiscountSource.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BillingDiscountRedemptionStatus.serializer(),
        BillingDiscountRedemptionStatus.UNKNOWN,
        mapOf(
          BillingDiscountRedemptionStatus.ACTIVE to "active",
          BillingDiscountRedemptionStatus.EXHAUSTED to "exhausted",
          BillingDiscountRedemptionStatus.REMOVED to "removed",
          BillingDiscountRedemptionStatus.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BillingStatementStatus.serializer(),
        BillingStatementStatus.UNKNOWN,
        mapOf(
          BillingStatementStatus.OPEN to "open",
          BillingStatementStatus.CLOSED to "closed",
          BillingStatementStatus.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BillingSubscriptionStatus.serializer(),
        BillingSubscriptionStatus.UNKNOWN,
        mapOf(
          BillingSubscriptionStatus.ACTIVE to "active",
          BillingSubscriptionStatus.ENDED to "ended",
          BillingSubscriptionStatus.UPCOMING to "upcoming",
          BillingSubscriptionStatus.PAST_DUE to "past_due",
          BillingSubscriptionStatus.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BillingSubscriptionPlanPeriod.serializer(),
        BillingSubscriptionPlanPeriod.UNKNOWN,
        mapOf(
          BillingSubscriptionPlanPeriod.MONTH to "month",
          BillingSubscriptionPlanPeriod.ANNUAL to "annual",
          BillingSubscriptionPlanPeriod.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BillingPaymentMethodStatus.serializer(),
        BillingPaymentMethodStatus.UNKNOWN,
        mapOf(
          BillingPaymentMethodStatus.ACTIVE to "active",
          BillingPaymentMethodStatus.EXPIRED to "expired",
          BillingPaymentMethodStatus.DISCONNECTED to "disconnected",
          BillingPaymentMethodStatus.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BillingPaymentChargeType.serializer(),
        BillingPaymentChargeType.UNKNOWN,
        mapOf(
          BillingPaymentChargeType.CHECKOUT to "checkout",
          BillingPaymentChargeType.RECURRING to "recurring",
          BillingPaymentChargeType.PRICE_TRANSITION to "price_transition",
          BillingPaymentChargeType.UNKNOWN to "unknown",
        ),
      ),
      ServerEnum(
        BillingPaymentStatus.serializer(),
        BillingPaymentStatus.UNKNOWN,
        mapOf(
          BillingPaymentStatus.PENDING to "pending",
          BillingPaymentStatus.PAID to "paid",
          BillingPaymentStatus.FAILED to "failed",
          BillingPaymentStatus.UNKNOWN to "unknown",
        ),
      ),
    )

  private val jsonWithoutCoercion = Json

  @Test
  fun `every server enum decodes an unrecognized value to its fallback`() {
    serverEnumsWithWireNames.forEach { assertUnknownFallsBack(it) }
  }

  @Test
  fun `every server enum round-trips its wire names`() {
    serverEnumsWithWireNames.forEach { assertRoundTrips(it) }
  }

  @Test
  fun `unknown values inside lists and nullable positions fall back instead of failing`() {
    val list =
      jsonWithoutCoercion.decodeFromString(
        ListSerializer(SignUp.Status.serializer()),
        """["complete","brand_new_status"]""",
      )
    assertEquals(listOf(SignUp.Status.COMPLETE, SignUp.Status.UNKNOWN), list)

    val nullable =
      jsonWithoutCoercion.decodeFromString(
        BillingDiscountRedemptionStatus.serializer().nullable,
        "\"brand_new_status\"",
      )
    assertEquals(BillingDiscountRedemptionStatus.UNKNOWN, nullable)
  }

  @Test
  fun `invitation with an unrecognized status decodes instead of failing the response`() {
    val json =
      """
      {"id":"orginv_1","email_address":"a@b.c","public_metadata":{},"role":"org:member",
       "status":"brand_new_status","created_at":1,"updated_at":2}
      """
        .trimIndent()

    val invitation = ClerkApi.json.decodeFromString(OrganizationInvitation.serializer(), json)

    assertEquals(OrganizationInvitation.Status.Unknown, invitation.status)
  }

  private fun <E : Enum<E>> assertUnknownFallsBack(case: ServerEnum<E>) {
    listOf(jsonWithoutCoercion, ClerkApi.json).forEach { json ->
      assertEquals(
        case.serializer.descriptor.serialName,
        case.fallback,
        json.decodeFromString(case.serializer, "\"brand_new_value_from_the_server\""),
      )
    }
  }

  private fun <E : Enum<E>> assertRoundTrips(case: ServerEnum<E>) {
    case.wireNames.forEach { (entry, wireName) ->
      val encoded = ClerkApi.json.encodeToString(case.serializer, entry)
      assertEquals("${case.serializer.descriptor.serialName}.$entry", "\"$wireName\"", encoded)
      assertEquals(entry, jsonWithoutCoercion.decodeFromString(case.serializer, encoded))
    }
  }
}
