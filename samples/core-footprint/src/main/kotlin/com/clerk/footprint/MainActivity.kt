package com.clerk.footprint

import android.app.Activity
import android.app.usage.StorageStatsManager
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.Log
import android.widget.TextView
import kotlinx.coroutines.*
import org.json.JSONObject

class MainActivity : Activity() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var owner: AutoCloseable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val label = TextView(this).apply { text = "Starting" }
    setContentView(label)
    scope.launch {
      try {
        owner = startOwner(this@MainActivity)
        label.text = "Ready"
        Log.i("ClerkCoreFootprint", "$packageName ready")
        if (Build.VERSION.SDK_INT >= 26) {
          val stats =
            getSystemService(StorageStatsManager::class.java)
              .queryStatsForUid(StorageManager.UUID_DEFAULT, applicationInfo.uid)
          Log.i(
            "ClerkCoreFootprint",
            JSONObject()
              .apply {
                put("package", packageName)
                put("appBytes", stats.appBytes)
                put("dataBytes", stats.dataBytes)
                put("cacheBytes", stats.cacheBytes)
                put("sdk", Build.VERSION.SDK_INT)
                put("abi", Build.SUPPORTED_ABIS.first())
                put("definition", "StorageStatsManager own-UID sample after first ready")
              }
              .toString(),
          )
        }
      } catch (error: Exception) {
        label.text = "Failed: ${error.javaClass.simpleName}"
        Log.e("ClerkCoreFootprint", "$packageName failed", error)
      }
    }
  }

  override fun onDestroy() {
    scope.cancel()
    owner?.close()
    super.onDestroy()
  }
}
