# Clerk Android Constants and Paths Consolidation Proposal

## Current Issues

1. **Duplication and Split**: Path constants are split between `Constants.Paths` (2 constants) and `Paths.kt` (all other paths)
2. **Naming Inconsistencies**: Mixed use of "Path" suffix in object names, inconsistent visibility modifiers
3. **Organization**: Parameter names mixed with paths, duplication of strategy keys
4. **Dependencies**: Circular-like dependencies between Constants and Paths files

## Proposed Structure

### 1. Consolidate All Paths into Single Location

**File: `source/api/src/main/kotlin/com/clerk/api/network/ApiPaths.kt`**

```kotlin
package com.clerk.api.network

/**
 * Internal object containing all API endpoint path constants for the Clerk REST API.
 * 
 * All paths are relative to the API base URL and version prefix. The paths are used by Retrofit
 * interfaces to define REST API endpoints.
 * 
 * This is an internal utility and should not be used outside the Clerk SDK.
 */
internal object ApiPaths {
  
  /** Client-related API endpoints */
  internal object Client {
    internal const val BASE = "client"
    
    /** Device attestation endpoints */
    internal object DeviceAttestation {
      internal const val BASE = "${Client.BASE}/device_attestation"
      internal const val CHALLENGES = "${BASE}/challenges"
      internal const val VERIFY = "${Client.BASE}/verify"
    }
    
    /** Session management endpoints */
    internal object Sessions {
      internal const val BASE = "${Client.BASE}/sessions"
      internal const val WITH_ID = "${BASE}/{id}"
      internal const val REMOVE = "${WITH_ID}/remove"
      internal const val TOKENS = "${WITH_ID}/tokens"
      internal const val TOKEN_TEMPLATE = "${TOKENS}/{template}"
      internal const val SET_ACTIVE = "${WITH_ID}/touch"
    }
    
    /** Sign-in endpoints */
    internal object SignIn {
      internal const val BASE = "${Client.BASE}/sign_ins"
      internal const val WITH_ID = "${BASE}/{id}"
      internal const val ATTEMPT_FIRST_FACTOR = "${WITH_ID}/attempt_first_factor"
      internal const val ATTEMPT_SECOND_FACTOR = "${WITH_ID}/attempt_second_factor"
      internal const val PREPARE_FIRST_FACTOR = "${WITH_ID}/prepare_first_factor"
      internal const val PREPARE_SECOND_FACTOR = "${WITH_ID}/prepare_second_factor"
      internal const val RESET_PASSWORD = "${WITH_ID}/reset_password"
    }
    
    /** Sign-up endpoints */
    internal object SignUp {
      internal const val BASE = "${Client.BASE}/sign_ups"
      internal const val WITH_ID = "${BASE}/{id}"
      internal const val PREPARE_VERIFICATION = "${WITH_ID}/prepare_verification"
      internal const val ATTEMPT_VERIFICATION = "${WITH_ID}/attempt_verification"
    }
  }
  
  /** Environment configuration endpoint */
  internal const val ENVIRONMENT = "environment"
  
  /** User-related endpoints */
  internal object User {
    internal const val BASE = "me"
    internal const val PROFILE_IMAGE = "${BASE}/profile_image"
    internal const val ACCEPT_ORG_INVITATION = "${BASE}/organization_invitations/{invitation_id}/accept"
    internal const val ACCEPT_ORG_SUGGESTION = "${BASE}/organization_suggestions/{suggestion_id}/accept"
    internal const val BACKUP_CODES = "${BASE}/backup_codes"
    
    /** Password management */
    internal object Password {
      internal const val UPDATE = "${User.BASE}/change_password"
      internal const val DELETE = "${User.BASE}/remove_password"
    }
    
    /** Session management */
    internal object Sessions {
      internal const val BASE = "${User.BASE}/sessions"
      internal const val REVOKE = "${BASE}/{session_id}/revoke"
      internal const val ACTIVE = "${BASE}/active"
    }
    
    /** Email address management */
    internal object EmailAddress {
      internal const val BASE = "${User.BASE}/email_addresses"
      internal const val WITH_ID = "${BASE}/{email_id}"
      internal const val ATTEMPT_VERIFICATION = "${WITH_ID}/attempt_verification"
      internal const val PREPARE_VERIFICATION = "${WITH_ID}/prepare_verification"
    }
    
    /** Phone number management */
    internal object PhoneNumber {
      internal const val BASE = "${User.BASE}/phone_numbers"
      internal const val WITH_ID = "${BASE}/{phone_number_id}"
      internal const val ATTEMPT_VERIFICATION = "${WITH_ID}/attempt_verification"
      internal const val PREPARE_VERIFICATION = "${WITH_ID}/prepare_verification"
    }
    
    /** Passkey management */
    internal object Passkey {
      internal const val BASE = "${User.BASE}/passkeys"
      internal const val WITH_ID = "${BASE}/{passkey_id}"
      internal const val ATTEMPT_VERIFICATION = "${WITH_ID}/attempt_verification"
    }
    
    /** External account management */
    internal object ExternalAccount {
      internal const val BASE = "${User.BASE}/external_accounts"
      internal const val WITH_ID = "${BASE}/{external_account_id}"
      internal const val REAUTHORIZE = "${WITH_ID}/reauthorize"
      internal const val REVOKE_TOKENS = "${WITH_ID}/tokens"
    }
    
    /** TOTP management */
    internal object TOTP {
      internal const val BASE = "${User.BASE}/totp"
      internal const val ATTEMPT_VERIFICATION = "${BASE}/attempt_verification"
    }
  }
  
  /** Organization endpoints */
  internal object Organization {
    internal const val BASE = "organizations"
    internal const val WITH_ID = "${BASE}/{organization_id}"
    internal const val LOGO = "${WITH_ID}/logo"
    internal const val ROLES = "${WITH_ID}/roles"
    
    /** Domain management */
    internal object Domain {
      internal const val BASE = "${Organization.WITH_ID}/domains"
      internal const val WITH_ID = "${BASE}/{domain_id}"
      internal const val UPDATE_ENROLLMENT_MODE = "${WITH_ID}/update_enrollment_mode"
      internal const val PREPARE_AFFILIATION = "${WITH_ID}/prepare_affiliation_verification"
      internal const val ATTEMPT_AFFILIATION = "${WITH_ID}/attempt_affiliation_verification"
    }
  }
}
```

### 2. Create Separate Parameter Constants File

**File: `source/api/src/main/kotlin/com/clerk/api/network/ApiParams.kt`**

```kotlin
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
  internal const val INVITATION_ID = "invitation_id"
  internal const val SUGGESTION_ID = "suggestion_id"
  internal const val TEMPLATE = "template"
  
  // Request parameters
  internal const val STRATEGY = "strategy"
  internal const val CODE = "code"
  internal const val CLERK_SESSION_ID = "_clerk_session_id"
}
```

### 3. Clean Up Constants.kt

**Remove duplicated constants and organize better:**

```kotlin
package com.clerk.api

import com.clerk.sdk.BuildConfig

/** Consolidated constants used throughout the Clerk SDK */
object Constants {

  /** Authentication and verification strategies */
  object Strategy {
    const val PHONE_CODE = "phone_code"
    const val EMAIL_CODE = "email_code"
    const val TOTP = "totp"
    const val BACKUP_CODE = "backup_code"
    const val PASSWORD = "password"
    const val PASSKEY = "passkey"
    const val RESET_PASSWORD_EMAIL_CODE = "reset_password_email_code"
    const val RESET_PASSWORD_PHONE_CODE = "reset_password_phone_code"
    const val TICKET = "ticket"
    const val TRANSFER = "transfer"
    const val ENTERPRISE_SSO = "enterprise_sso"
  }

  /** HTTP and API related constants */
  object Http {
    const val NO_CONTENT = 204
    const val RESET_CONTENT = 205
    const val CURRENT_API_VERSION = "2024-10-01"
    const val CURRENT_SDK_VERSION = BuildConfig.SDK_VERSION
    const val IS_MOBILE_HEADER_VALUE = "1"
    const val AUTHORIZATION_HEADER = "Authorization"
    const val IS_NATIVE_QUERY_PARAM = "_is_native"
  }

  /** Configuration and timing constants */
  object Config {
    const val REFRESH_TOKEN_INTERVAL = 5
    const val API_TIMEOUT_SECONDS = 30L
    const val TIMEOUT_MULTIPLIER = 1000
    const val BACKOFF_BASE_DELAY_SECONDS = 5L
    const val MAX_ATTESTATION_RETRIES = 3
    const val EXPONENTIAL_BACKOFF_SHIFT = 1
    const val DEFAULT_EXPIRATION_BUFFER = 1000L
    const val COMPRESSION_PERCENTAGE = 75
  }

  /** URL and key prefixes */
  object Prefixes {
    const val URL_SSL_PREFIX = "https://"
    const val TOKEN_PREFIX_LIVE = "pk_live_"
    const val TOKEN_PREFIX_TEST = "pk_test_"
  }

  /** Storage and preference keys */
  object Storage {
    const val CLERK_PREFERENCES_FILE_NAME = "clerk_preferences"
    const val KEY_AUTHORIZATION_STARTED = "authStarted"
  }

  /** Device attestation constants */
  object Attestation {
    const val HASH_CONSTANT = 0xff
    const val PREPARATION_TIMEOUT_MS = 30_000L // 30 seconds
    const val ATTESTATION_TIMEOUT_MS = 15_000L // 15 seconds
    const val HASH_CACHE_MAX_SIZE = 100
    const val SHA256_HEX_LENGTH = 64
  }

  /** Parameter and field names */
  object Fields {
    const val STRATEGY = "strategy"
  }

  /** Test constants (only available in test builds) */
  object Test {
    const val CONCURRENCY_TEST_THREAD_COUNT = 10
    const val EXPECTED_STORAGE_LOAD_CALLS = 1
    const val EXPECTED_STORAGE_SAVE_CALLS = 1
    const val EXPECTED_SAVED_DEVICE_ID_COUNT = 1
  }
}
```

## Benefits of This Approach

1. **Single Source of Truth**: All API paths in one location (`ApiPaths.kt`)
2. **Consistent Naming**: All objects use consistent naming without "Path" suffix
3. **Better Organization**: Paths and parameters are logically separated
4. **Reduced Duplication**: Eliminates duplicate `STRATEGY_KEY` constants
5. **Cleaner Dependencies**: No circular dependencies between files
6. **Improved Maintainability**: Easier to find and update related constants

## Migration Steps

1. Create new `ApiPaths.kt` and `ApiParams.kt` files
2. Update all imports throughout codebase
3. Remove `Constants.Paths` object
4. Remove duplicate constants from `Constants.kt`
5. Delete old `Paths.kt` file
6. Update any remaining references

## Breaking Changes

This is an internal API refactoring with no public API changes. All changes are in internal classes and objects.