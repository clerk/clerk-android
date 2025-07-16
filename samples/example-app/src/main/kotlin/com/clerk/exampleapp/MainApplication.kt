package com.clerk.exampleapp

import android.app.Application
import com.clerk.Clerk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    Clerk.initialize(this, "pk_test_Z2xvd2luZy1zcXVpZC0xMS5jbGVyay5hY2NvdW50cy5kZXYk")
  }
}
