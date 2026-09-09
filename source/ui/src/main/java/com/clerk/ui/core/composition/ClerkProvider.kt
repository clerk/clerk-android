package com.clerk.ui.core.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.ui.theme.ClerkTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import java.util.UUID

private data class ObservedClerk(val clerk: Clerk, val revision: Long)

private val LocalObservedClerk = compositionLocalOf<ObservedClerk?> { null }

/** Supplies the application's generated Clerk instance to prebuilt Compose UI. */
@Composable
fun ClerkProvider(clerk: Clerk, theme: ClerkTheme? = null, content: @Composable () -> Unit) {
  // Observe revisions, including nested resource changes whose root handle stays identical.
  val runtime = remember(clerk) { clerk.context.requireRuntime() }
  val revision by runtime.changes.collectAsState()
  CompositionLocalProvider(LocalObservedClerk provides ObservedClerk(clerk, revision)) {
    ClerkThemeOverrideProvider(theme, content)
  }
}

object LocalClerk {
  val current: Clerk
    @Composable get() = currentOrNull ?: error("Wrap Clerk UI in ClerkProvider(clerk).")

  val currentOrNull: Clerk?
    @Composable get() = LocalObservedClerk.current?.clerk
}

/** Each UI model belongs to the supplied core, including when the provider changes instances. */
@Composable
internal inline fun <reified T : ViewModel> clerkViewModel(crossinline create: (Clerk) -> T): T {
  val clerk = LocalClerk.current
  val ownerKey = remember(clerk) { UUID.randomUUID().toString() }
  return viewModel(key = "${T::class.java.name}:$ownerKey") { create(clerk) }
}
