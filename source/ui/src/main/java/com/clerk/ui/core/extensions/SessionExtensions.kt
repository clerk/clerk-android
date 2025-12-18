package com.clerk.ui.core.extensions

import com.clerk.api.session.Session
import com.clerk.api.session.SessionActivity
import com.clerk.ui.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

internal fun SessionActivity.deviceImage(): Int {
  return if (this.isMobile == true) {
    R.drawable.ic_mobile
  } else {
    R.drawable.ic_desktop
  }
}

internal fun SessionActivity.deviceText(): Int {
  return if (this.isMobile == true) {
    R.string.mobile_device
  } else {
    R.string.desktop_device
  }
}

internal val SessionActivity.browserFormatted
  get() = joinNonNull(browserName, browserVersion)
internal val SessionActivity.locationFormatted
  get() = joinNonNull(city, country, separator = ", ")
internal val SessionActivity.ipAndLocationFormatted
  get() = joinNonNull(ipAddress, "(${locationFormatted})")

internal val Session.lastActiveRelativeTime
  get() = formattedRelativeDateTime(this.lastActiveAt)

private fun joinNonNull(vararg parts: String?, separator: String = " ") =
  parts.filterNotNull().joinToString(separator)

internal fun formattedRelativeDateTime(timestampMillis: Long): String {
  val now = System.currentTimeMillis()
  val diff = now - timestampMillis
  val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }

  val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
  val dateFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())

  return when {
    TimeUnit.MILLISECONDS.toDays(diff) == 0L -> "Today, ${timeFormat.format(calendar.time)}"
    TimeUnit.MILLISECONDS.toDays(diff) == 1L -> "Yesterday, ${timeFormat.format(calendar.time)}"
    else -> dateFormat.format(calendar.time)
  }
}
