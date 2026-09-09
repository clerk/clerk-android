package com.clerk.ui.userprofile.security.device

import com.clerk.api.SessionWithActivities

internal fun List<SessionWithActivities>.sortedForDeviceDisplay(
  currentSessionId: String?
): List<SessionWithActivities> =
  sortedWith(
    compareByDescending<SessionWithActivities> { it.id == currentSessionId }
      .thenByDescending { it.lastActiveAt }
  )
