package com.clerk.api.network

/**
 * Common parameter names used in API paths and requests.
 *
 * This object contains constants for parameter names that are frequently used across different API
 * endpoints. Using these constants ensures consistency and reduces the risk of typos.
 */
internal object ApiParams {
  // Path parameters
  internal const val ID = "id"
  internal const val SESSION_ID = "session_id"
  internal const val EMAIL_ID = "email_id"
  internal const val PHONE_NUMBER_ID = "phone_number_id"
  internal const val PASSKEY_ID = "passkey_id"
  internal const val EXTERNAL_ACCOUNT_ID = "external_account_id"
  internal const val ORGANIZATION_ID = "organization_id"
  internal const val DOMAIN_ID = "domain_id"
  internal const val USER_ID = "user_id"
  internal const val INVITATION_ID = "invitation_id"
  internal const val SUGGESTION_ID = "suggestion_id"
  internal const val TEMPLATE = "template"

  // Request parameters
  internal const val STRATEGY = "strategy"
  internal const val CODE = "code"
  internal const val CLERK_SESSION_ID = "_clerk_session_id"

  internal const val ROLE = "role"

  // Query parameters
  internal const val LIMIT = "limit"
  internal const val OFFSET = "offset"
}
