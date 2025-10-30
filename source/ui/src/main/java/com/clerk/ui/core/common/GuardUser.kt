package com.clerk.ui.core.common

import com.clerk.api.Clerk
import com.clerk.api.user.User

internal fun guardUser(userDoesNotExist: () -> Unit, block: (User) -> Unit) {
  val u = Clerk.user
  if (u == null) {
    userDoesNotExist()
  } else {
    block(u)
  }
}
