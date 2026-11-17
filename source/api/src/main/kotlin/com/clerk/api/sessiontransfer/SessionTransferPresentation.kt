package com.clerk.api.sessiontransfer

/** Where the app opens a session transfer URL. FAPI tailors the transfer to this surface. */
public enum class SessionTransferPresentation(internal val value: String) {
  /** The app loads the URL into its own `android.webkit.WebView`. */
  WEB_VIEW("webview"),

  /** The app opens the URL in the user's default browser. */
  BROWSER("browser"),
}
