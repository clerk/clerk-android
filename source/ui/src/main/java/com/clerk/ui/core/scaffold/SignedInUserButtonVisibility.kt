package com.clerk.ui.core.scaffold

import com.clerk.api.session.Session
import com.clerk.api.user.User

internal fun shouldShowSignedInUserButton(
  showSignedInUserButton: Boolean,
  user: User?,
  session: Session?,
): Boolean =
  showSignedInUserButton && user != null && session?.status == Session.SessionStatus.PENDING
