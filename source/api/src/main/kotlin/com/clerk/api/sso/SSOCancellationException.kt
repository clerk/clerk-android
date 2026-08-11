package com.clerk.api.sso

/**
 * Signals that a redirect-based OAuth or SSO authentication attempt ended without completing.
 *
 * Check `ClerkResult.Failure.throwable` against this type to distinguish a browser dismissal or
 * cancelled provider flow from an authentication failure.
 */
public class SSOCancellationException internal constructor(message: String) : Exception(message)
