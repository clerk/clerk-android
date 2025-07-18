package com.clerk.quickstart

import android.app.Application
import com.clerk.Clerk

class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    Clerk.initialize(this)
  }
}
