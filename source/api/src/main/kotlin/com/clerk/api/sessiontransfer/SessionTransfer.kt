package com.clerk.api.sessiontransfer

import android.net.Uri

/**
 * A single-use URL that signs the current user in on the web and then lands on the destination it
 * was prepared for.
 *
 * Create one with `Clerk.auth.prepareSessionTransfer` right before opening it.
 */
public class SessionTransfer
internal constructor(
  /** The URL to open. It is single use and expires at [expiresAt]. Never log or store it. */
  public val url: Uri,
  /** When [url] stops working, in milliseconds since the Unix epoch. */
  public val expiresAt: Long,
) {
  /** Describes this transfer without its URL. */
  override fun toString(): String = "SessionTransfer(url=██, expiresAt=$expiresAt)"
}
