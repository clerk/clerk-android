package com.clerk.linearclone

import android.app.Application
import com.clerk.Clerk
import com.clerk.ClerkConfigurationOptions

class LinearCloneApp : Application() {

  override fun onCreate() {
    super.onCreate()
    Clerk.initialize(
      this,
      "pk_test_c3VpdGVkLXBvbGxpd29nLTg5LmNsZXJrLmFjY291bnRzLmRldiQ",
      ClerkConfigurationOptions(enableDebugMode = true),
    )
  }
}
