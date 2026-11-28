package com.clerk.ui.reverification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

internal class ReverificationActivity : ComponentActivity() {
  private var request: ReverificationRequest? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val request =
      intent.getStringExtra(EXTRA_REQUEST_ID)?.let(ReverificationPresenter.shared::request)
    if (request == null || request.isFinished) {
      finish()
      return
    }
    this.request = request
    enableEdgeToEdge()
    lifecycleScope.launch {
      request.awaitEnd()
      finish()
    }
    setContent {
      ReverificationView(
        level = request.level,
        sessionId = request.identity.sessionId,
        onVerified = { ReverificationPresenter.shared.complete(request, it) },
        onClose = ::finish,
      )
    }
  }

  override fun finish() {
    request?.let(ReverificationPresenter.shared::dismiss)
    super.finish()
  }

  override fun onDestroy() {
    if (isFinishing) request?.let(ReverificationPresenter.shared::dismiss)
    super.onDestroy()
  }

  companion object {
    private const val EXTRA_REQUEST_ID = "com.clerk.ui.reverification.REQUEST_ID"

    fun intent(context: Context, requestId: String): Intent =
      Intent(context, ReverificationActivity::class.java).putExtra(EXTRA_REQUEST_ID, requestId)
  }
}
