package com.clerk.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import kotlin.test.assertSame
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

  @Test
  fun componentKeepsItsViewModelsWhenHostNavigationTakesItOffScreenAndBack() {
    val hostBackStack = mutableStateListOf<String>("component")
    val componentBackStack = mutableStateListOf<String>("root", "detail")
    val created = mutableMapOf<String, TrackingViewModel>()

    composeTestRule.setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides hostOwner) {
        NavDisplay(
          backStack = hostBackStack,
          entryDecorators = rememberClerkNavEntryDecorators(),
          entryProvider = { hostKey ->
            NavEntry(hostKey) {
              if (hostKey == "component") {
                ClerkViewModelStoreScope {
                  created["component"] = viewModel()
                  NavDisplay(
                    backStack = componentBackStack,
                    entryDecorators = rememberClerkNavEntryDecorators(),
                    entryProvider = { key -> NavEntry(key) { created[key] = viewModel() } },
                  )
                }
              }
            }
          },
        )
      }
    }
    composeTestRule.waitForIdle()
    val component = created.getValue("component")
    val detail = created.getValue("detail")

    // The host app navigates forward, then back.
    composeTestRule.runOnIdle { hostBackStack.add("elsewhere") }
    composeTestRule.waitForIdle()
    assertFalse(component.cleared)
    assertFalse(detail.cleared)

    composeTestRule.runOnIdle { hostBackStack.removeAt(hostBackStack.lastIndex) }
    composeTestRule.waitForIdle()
    assertSame(component, created.getValue("component"))
    assertSame(detail, created.getValue("detail"))
    assertFalse(component.cleared)
    assertFalse(detail.cleared)

    // Popping the host entry that holds the component does clear it.
    composeTestRule.runOnIdle {
      hostBackStack.add("elsewhere")
      hostBackStack.removeAt(0)
    }
    composeTestRule.waitForIdle()
    assertTrue(component.cleared)
    assertTrue(detail.cleared)
  }

  @Test
  fun componentInASavedTabKeepsItsViewModelsAcrossTabSwitches() {
    var selectedTab by mutableStateOf("clerk")
    val created = mutableListOf<TrackingViewModel>()

    composeTestRule.setContent {
      CompositionLocalProvider(LocalViewModelStoreOwner provides hostOwner) {
        val tabs = rememberSaveableStateHolder()
        tabs.SaveableStateProvider(selectedTab) {
          if (selectedTab == "clerk") {
            ClerkViewModelStoreScope { created += viewModel<TrackingViewModel>() }
          }
        }
      }
    }
    composeTestRule.waitForIdle()
    val first = created.last()

    composeTestRule.runOnIdle { selectedTab = "other" }
    composeTestRule.waitForIdle()
    assertFalse(first.cleared)

    composeTestRule.runOnIdle { selectedTab = "clerk" }
    composeTestRule.waitForIdle()
    assertSame(first, created.last())
    assertFalse(first.cleared)
  }

  class TrackingViewModel : ViewModel() {
    var cleared = false
      private set

    override fun onCleared() {
      cleared = true
    }
  }
}
