package com.clerk.ui.navigation

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
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
 * store, so ViewModels outlive the component and leak state into the next time it is shown. The
 * store provided here survives configuration changes (it is held by a ViewModel in the parent
 * store) and is cleared when the component leaves composition for good.
 *
 * Leaving composition is not always for good: host navigation (a `NavHost` destination the user
 * navigates away from, a bottom-nav tab saved with `saveState`, a Navigation 3 entry that is no
 * longer on top) disposes the component but saves its state, including the scope key and the
 * component's inner back stack, and restores both when it comes back. Clearing then would hand the
 * restored back stack fresh ViewModels and drop in-flight work. So the store is only cleared on
 * dispose when the surrounding [androidx.compose.runtime.saveable.SaveableStateRegistry] did not
 * save the scope key as part of the disposal. A saved scope's store instead lives until the host
 * clears the parent store (the host entry is popped, or the Activity finishes).
 */
@Composable
internal fun ClerkViewModelStoreScope(content: @Composable () -> Unit) {
  val parent = LocalViewModelStoreOwner.current
  if (parent == null) {
    // Previews and some snapshot hosts have no owner; nothing to scope against.
    content()
    return
  }
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
      // A host that saves this scope as it disposes it (navigation, saved tabs) can restore the
      // same key later, so its ViewModels must survive until the host clears the parent store.
      if (!saveTracker.savedDuringCurrentMessage && shouldClearOnDispose()) {
        stores.clear(scopeKey)
      }
    }
  }
  CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}

/**
 * Entry decorators for Clerk's [androidx.navigation3.ui.NavDisplay]s: saveable state plus a
 * [ViewModelStore] per back stack entry, so a screen's ViewModels are cleared when it is popped.
 */
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
 * [androidx.compose.runtime.saveable. SaveableStateRegistry].
 *
 * A `SaveableStateHolder` saves an entry's state in its own dispose callback, which runs before the
 * dispose callbacks of the entry's content, all within one main-thread message. The flag is reset
 * on the next message, so a save from an earlier `onSaveInstanceState` (or from registration) is
 * never mistaken for a save that accompanies the current disposal.
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
      (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras
        ?: CreationExtras.Empty
}
