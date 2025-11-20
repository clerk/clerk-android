package com.clerk.api.ui

import androidx.compose.ui.text.TextStyle

/**
 * A container for Clerk's typography styles, designed to integrate with Jetpack Compose.
 *
 * This class provides a way to customize the text styles used throughout the Clerk UI components.
 * It mirrors a subset of the Material 3 typography scale, allowing you to override specific styles
 * by providing your own [TextStyle] objects.
 *
 * Any `TextStyle` not provided will fall back to Clerk's default styling.
 *
 * The available typography slots correspond to different levels of textual hierarchy:
 * - `headlineLarge`, `headlineMedium`, `headlineSmall`
 * - `titleMedium`, `titleSmall`
 * - `bodyLarge`, `bodyMedium`, `bodySmall`
 * - `labelMedium`, `labelSmall`
 *
 * @property displaySmall A custom style for small display text.
 * @property headlineLarge A custom style for large headline text.
 * @property headlineMedium A custom style for medium headline text.
 * @property headlineSmall A custom style for small headline text.
 * @property titleMedium A custom style for medium title text.
 * @property titleSmall A custom style for small title text.
 * @property bodyLarge A custom style for large body text.
 * @property bodyMedium A custom style for medium body text.
 * @property bodySmall A custom style for small body text.
 * @property labelMedium A custom style for medium label text.
 * @property labelSmall A custom style for small label text.
 */
class ClerkTypography(
  val displaySmall: TextStyle? = null,
  val headlineLarge: TextStyle? = null,
  val headlineMedium: TextStyle? = null,
  val headlineSmall: TextStyle? = null,
  val titleMedium: TextStyle? = null,
  val titleSmall: TextStyle? = null,
  val bodyLarge: TextStyle? = null,
  val bodyMedium: TextStyle? = null,
  val bodySmall: TextStyle? = null,
  val labelMedium: TextStyle? = null,
  val labelSmall: TextStyle? = null,
)
