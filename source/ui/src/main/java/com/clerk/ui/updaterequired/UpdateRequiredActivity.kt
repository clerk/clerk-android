package com.clerk.ui.updaterequired

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clerk.api.Clerk
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class UpdateRequiredActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          moveTaskToBack(true)
        }
      },
    )
    val resetsAtStart = Clerk.appVersionSupportResets.value
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        combine(
            Clerk.isInitialized,
            Clerk.appVersionSupportStatus,
            Clerk.appVersionSupportResets,
          ) { initialized, status, resets ->
            (initialized && status.isSupported) || resets != resetsAtStart
          }
          .first { it }
        finish()
      }
    }
    setContent { UpdateRequiredView() }
  }
}
