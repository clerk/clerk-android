package com.clerk.api.billing

import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.billing.BillingSubscriptionItem
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken

/**
 * Internal service that registers Google Play purchases with Clerk.
 *
 * Registration posts the purchase token to `POST /me/commerce/store_purchases`, where Clerk
 * verifies it against the Play Developer API, binds it to the current payer, activates the
 * subscription item, and acknowledges the purchase. The SDK intentionally never acknowledges
 * purchases client-side — server-side acknowledgment is the source of truth, and unverified
 * purchases must be left unacknowledged so Google auto-refunds them.
 *
 * The endpoint is idempotent per store transaction lineage, so restore flows and out-of-band
 * purchase events can safely re-register the same purchase.
 */
internal class StorePurchaseRegistrar {
  /**
   * Registers the given Google Play [purchaseToken] with Clerk.
   *
   * On success, the session token is force-refreshed so entitlements granted by the new
   * subscription (`fea` claims) become live immediately.
   */
  suspend fun register(purchaseToken: String): ClerkResult<BillingSubscriptionItem, BillingError> {
    return when (
      val result =
        ClerkApi.commerce.createStorePurchase(store = STORE_GOOGLE, payload = purchaseToken)
    ) {
      is ClerkResult.Success -> {
        refreshSessionToken()
        ClerkResult.success(result.value)
      }
      is ClerkResult.Failure -> {
        ClerkLog.w("Failed to register store purchase with Clerk: ${result.error}")
        ClerkResult.apiFailure(result.toBillingError())
      }
    }
  }

  /** Forces a session token refresh so newly granted `fea` claims land immediately. */
  private suspend fun refreshSessionToken() {
    Clerk.session?.fetchToken(GetTokenOptions(skipCache = true))
  }

  internal companion object {
    /** The `store` value Clerk expects for Google Play purchases. */
    internal const val STORE_GOOGLE = "google"
  }
}
