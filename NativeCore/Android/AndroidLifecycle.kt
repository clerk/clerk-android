package com.clerk.api

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.lang.ref.WeakReference

internal fun observeApplicationLifecycle(runtime: CoreRuntime) {
  val runtimeReference = WeakReference(runtime)
  val lifecycle = ProcessLifecycleOwner.get().lifecycle
  val observer = object : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) { runtimeReference.get()?.setApplicationActive(true) }
    override fun onStop(owner: LifecycleOwner) { runtimeReference.get()?.setApplicationActive(false) }
  }
  lifecycle.addObserver(observer)
  runtime.setApplicationActive(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
  runtime.addTeardown { lifecycle.removeObserver(observer) }
}
