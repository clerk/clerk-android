package com.clerk.ui.core.scaffold

import com.clerk.api.session.Session

internal fun shouldShowSignedInUserButton(
  showSignedInUserButton: Boolean,
  sessionStatus: Session.SessionStatus?,
): Boolean = showSignedInUserButton && sessionStatus == Session.SessionStatus.PENDING
