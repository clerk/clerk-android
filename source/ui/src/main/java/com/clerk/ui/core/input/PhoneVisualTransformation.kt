package com.clerk.ui.core.input

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.google.i18n.phonenumbers.AsYouTypeFormatter
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlin.math.max
import kotlin.math.min

private val phoneUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

internal fun phoneVisualTransformation(regionIso: String): VisualTransformation =
  VisualTransformation { text ->
    // RAW: must already be filtered/capped (only '+' and digits)
    val raw = text.text

    val fmt: AsYouTypeFormatter = phoneUtil.getAsYouTypeFormatter(regionIso)
    fmt.clear()

    // rawToFmt[pos] = formatted length AFTER processing first `pos` raw chars (boundary map)
    val rawToFmt = IntArray(raw.length + 1)
    var formatted = ""
    rawToFmt[0] = 0
    for (i in 0 until raw.length) {
      val ch = raw[i]
      val out = if (ch == '+' || ch.isDigit()) fmt.inputDigit(ch) else formatted
      formatted = out
      rawToFmt[i + 1] = formatted.length
    }

    val fLen = formatted.length

    // Clamp any accidental overshoot (extra safety)
    for (i in 0..raw.length) {
      rawToFmt[i] = min(rawToFmt[i], fLen)
    }

    // Build reverse mapping for every formatted boundary f ∈ [0, fLen]
    val fmtToRaw = IntArray(fLen + 1)
    var r = 0
    for (f in 0..fLen) {
      while (r < rawToFmt.size && rawToFmt[r] < f) r++
      fmtToRaw[f] = max(0, min(r, raw.length))
    }

    val mapping =
      object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
          val o = offset.coerceIn(0, raw.length)
          // Always in [0, fLen]
          return rawToFmt[o]
        }

        override fun transformedToOriginal(offset: Int): Int {
          val f = offset.coerceIn(0, fLen)
          return fmtToRaw[f]
        }
      }

    TransformedText(AnnotatedString(formatted), mapping)
  }
