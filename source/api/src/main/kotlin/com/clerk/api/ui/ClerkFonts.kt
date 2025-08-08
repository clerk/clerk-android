package com.clerk.api.ui

/**
 * A container for a complete typography scale used by Clerk UI components.
 *
 * Each property represents a distinct text style that follows the
 * [Apple Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/typography#iOS)
 * naming convention:
 *
 * • `largeTitle` – Equivalent to a display-sized style, used for prominent headings. • `title`,
 * `title2`, `title3` – A progressively smaller set of title styles. • `headline` – Emphasised body
 * text, typically semibold. • `body` – Primary paragraph style for long-form text. • `callout` –
 * Slightly smaller than body; auxiliary text. • `subhead` – Secondary text used for grouping
 * related content. • `footnote` – Caption-like style for annotations. • `caption`, `caption2` – The
 * smallest type sizes, often used for metadata.
 *
 * The concrete rendering (font family, weight, letter-spacing, etc.) is defined by the [Font]
 * instances supplied to this class. Because different design systems or brands can provide their
 * own `Font` implementations, the SDK does not hard-code any particular Android `Typeface` here;
 * instead, it exposes a simple name-plus-size pair that higher-level modules can resolve to real
 * `TextStyle`s (Compose) or `Typeface`s (View system).
 */
class ClerkFonts(
  /** Largest title style, e.g. 34 sp Semibold. */
  val largeTitle: Font,
  /** Base title style. */
  val title: Font,
  /** Second-level title style. */
  val title2: Font,
  /** Third-level title style. */
  val title3: Font,
  /** Prominent body style suitable for headlines. */
  val headline: Font,
  /** Default body text style. */
  val body: Font,
  /** Slightly smaller than body for tertiary content. */
  val callout: Font,
  /** Label for grouped content sections. */
  val subhead: Font,
  /** Auxiliary notes such as table footers. */
  val footnote: Font,
  /** Smallest style for image captions etc. */
  val caption: Font,
  /** Even smaller variant of [caption] when required. */
  val caption2: Font,
)

/**
 * Simple value object describing an abstract font style.
 *
 * @property name Human-readable font family or PostScript name. This field is *not* constrained by
 *   the SDK; callers may provide any identifier that their own typography pipeline can recognise.
 * @property size Point size expressed as a floating-point number.
 */
data class Font(val name: String, val size: Double)
