package com.clerk.ui.userprofile.security.device

import com.clerk.api.session.Session
import com.clerk.api.session.isThisDevice

internal fun List<Session>.sortedForDeviceDisplay(): List<Session> =
  filter { it.latestActivity != null }
    .sortedWith(
      Comparator { a, b ->
        when {
          a.isThisDevice && !b.isThisDevice -> -1
          !a.isThisDevice && b.isThisDevice -> 1
          else -> b.lastActiveAt.compareTo(a.lastActiveAt)
        }
      }
    )
