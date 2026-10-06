package com.clerk.ui.navigation

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecoratorDefaults
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import java.util.UUID

private const val COMPONENT_STORES_KEY = "com.clerk.ui.navigation.ClerkComponentViewModelStores"

/**
 * Gives a prebuilt Clerk component its own [ViewModelStore].
 *
 * Without this, every `viewModel()` call inside a component resolves against the host Activity's
 * store, which is only cleared when the Activity finishes. The store provided here survives
 * configuration changes (it is held by a ViewModel in the parent store) and is cleared when the
 * component leaves composition.
 *
 * Host navigation (a `NavHost` destination, a Navigation 3 entry with a per-entry ViewModel store,
 * a tab saved with `saveState`) disposes a destination that is no longer shown but saves its state
 * through a `SaveableStateHolder` and restores it when the destination comes back. When the parent
 * owner is such a per-destination store, a scope that is saved as it is disposed keeps its store
 * until that parent is cleared. When the parent owner is the view tree's owner (the Activity or
 * Fragment), nothing would ever clear a kept store, so the scope is cleared on dispose regardless.
 */
@Composable
internal fun ClerkViewModelStoreScope(content: @Composable () -> Unit) {
  val parent = LocalViewModelStoreOwner.current
  if (parent == null) {
    content()
    return
  }
  val parentClearsKeptStore = parent !== LocalView.current.findViewTreeViewModelStoreOwner()
  val saveTracker = remember { ScopeKeySaveTracker() }
  val scopeKey = rememberSaveable(saver = saveTracker.saver) { UUID.randomUUID().toString() }
  val stores: ComponentViewModelStores =
    viewModel(viewModelStoreOwner = parent, key = COMPONENT_STORES_KEY)
  val shouldClearOnDispose = ViewModelStoreNavEntryDecoratorDefaults.removeViewModelStoreOnPop()
  val owner =
    remember(stores, scopeKey, parent) {
      ScopedViewModelStoreOwner(stores.storeFor(scopeKey), parent)
    }
  DisposableEffect(stores, scopeKey) {
    onDispose {
      val hostMayRestoreScope = saveTracker.savedDuringCurrentMessage && parentClearsKeptStore
      if (!hostMayRestoreScope && shouldClearOnDispose()) {
        stores.clear(scopeKey)
      }
    }
  }
  CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}

@Composable
internal fun <T : Any> rememberClerkNavEntryDecorators(): List<NavEntryDecorator<T>> {
  val saveableStateDecorator = rememberSaveableStateHolderNavEntryDecorator<T>()
  if (LocalViewModelStoreOwner.current == null) return listOf(saveableStateDecorator)
  return listOf(saveableStateDecorator, rememberViewModelStoreNavEntryDecorator())
}

internal class ComponentViewModelStores : ViewModel() {
  private val stores = mutableMapOf<String, ViewModelStore>()

  fun storeFor(key: String): ViewModelStore = stores.getOrPut(key) { ViewModelStore() }

  fun clear(key: String) {
    stores.remove(key)?.clear()
  }

  override fun onCleared() {
    stores.values.forEach { it.clear() }
    stores.clear()
  }
}

/**
 * Records whether the scope key was just saved by a
 * [androidx.compose.runtime.saveable.SaveableStateRegistry].
 *
 * A `SaveableStateHolder` saves an entry's state in its own dispose callback, which runs before the
 * dispose callbacks of the entry's content, all within one main-thread message. The flag is reset
 * on the next message, so a save from an earlier `onSaveInstanceState` is not mistaken for a save
 * that accompanies a later disposal.
 *
 * Before API 28 the platform calls `onSaveInstanceState` in the same message as `onStop`, so a
 * disposal that runs synchronously inside `onStop` still sees that save. Such a scope is kept,
 * which only happens under a per-destination parent, and is cleared when that parent is cleared.
 */
private class ScopeKeySaveTracker {
  private val mainHandler = Handler(Looper.getMainLooper())
  private val reset = Runnable { savedDuringCurrentMessage = false }

  var savedDuringCurrentMessage = false
    private set

  val saver: Saver<String, String> =
    Saver(
      save = { key ->
        savedDuringCurrentMessage = true
        mainHandler.removeCallbacks(reset)
        mainHandler.post(reset)
        key
      },
      restore = { it },
    )
}

private class ScopedViewModelStoreOwner(
  override val viewModelStore: ViewModelStore,
  private val parent: ViewModelStoreOwner,
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
  override val defaultViewModelProviderFactory: ViewModelProvider.Factory
    get() =
      (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelProviderFactory
        ?: ViewModelProvider.NewInstanceFactory()

  override val defaultViewModelCreationExtras: CreationExtras
    get() =
      MutableCreationExtras(
          (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras
            ?: CreationExtras.Empty
        )
        .apply { set(VIEW_MODEL_STORE_OWNER_KEY, this@ScopedViewModelStoreOwner) }
}
