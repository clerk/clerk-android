package com.clerk.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClerkViewModelStoreScopeTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val hostOwner =
    object : ViewModelStoreOwner {
      override val viewModelStore = ViewModelStore()
    }

  @Test
  fun viewModelsAreClearedWhenTheComponentLeavesComposition() {
    var showComponent by mutableStateOf(true)
    val created = mutableListOf<TrackingViewModel>()

    composeTestRule.setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides hostOwner) {
        if (showComponent) {
          ClerkViewModelStoreScope { created += viewModel<TrackingViewModel>() }
        }
      }
    }
    composeTestRule.waitForIdle()
    val first = created.last()
    assertFalse(first.cleared)

    composeTestRule.runOnIdle { showComponent = false }
    composeTestRule.waitForIdle()
    assertTrue(first.cleared)

    composeTestRule.runOnIdle { showComponent = true }
    composeTestRule.waitForIdle()
    assertNotSame(first, created.last())
  }

  @Test
  fun siblingComponentsDoNotShareViewModels() {
    val created = mutableListOf<TrackingViewModel>()

    composeTestRule.setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides hostOwner) {
        ClerkViewModelStoreScope { created += viewModel<TrackingViewModel>() }
        ClerkViewModelStoreScope { created += viewModel<TrackingViewModel>() }
      }
    }
    composeTestRule.waitForIdle()

    assertEquals(2, created.toSet().size)
  }

  @Test
  fun navEntryViewModelsAreClearedWhenTheEntryIsPopped() {
    val backStack = mutableStateListOf<String>("root", "detail")
    val created = mutableMapOf<String, TrackingViewModel>()

    composeTestRule.setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides hostOwner) {
        ClerkViewModelStoreScope {
          NavDisplay(
            backStack = backStack,
            entryDecorators = rememberClerkNavEntryDecorators(),
            entryProvider = { key -> NavEntry(key) { created[key] = viewModel() } },
          )
        }
      }
    }
    composeTestRule.waitForIdle()
    val detail = created.getValue("detail")

    composeTestRule.runOnIdle { backStack.removeAt(backStack.lastIndex) }
    composeTestRule.waitForIdle()

    assertTrue(detail.cleared)
    assertFalse(created.getValue("root").cleared)
  }

  class TrackingViewModel : ViewModel() {
    var cleared = false
      private set

    override fun onCleared() {
      cleared = true
    }
  }
}
