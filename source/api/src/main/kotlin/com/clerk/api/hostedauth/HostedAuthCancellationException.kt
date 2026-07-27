package com.clerk.api.hostedauth

/**
 * Signals that a hosted authentication attempt ended without completing, either because the user
 * dismissed the system browser or because another authentication flow superseded it.
 *
 * Check `ClerkResult.Failure.throwable` against this type to branch on cancellation without
 * matching message strings.
 */
public class HostedAuthCancellationException internal constructor(message: String) :
  Exception(message)
