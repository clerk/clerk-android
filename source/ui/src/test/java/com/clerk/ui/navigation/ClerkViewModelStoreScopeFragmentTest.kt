package com.clerk.ui.navigation

import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ClerkViewModelStoreScopeFragmentTest {

  private lateinit var activity: FragmentActivity
  private val containerId = View.generateViewId()

  @Before
  fun setUp() {
    activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
    activity.setContentView(FrameLayout(activity).apply { id = containerId })
  }

  @Test
  fun componentInAFragmentKeepsItsViewModelsWhileTheFragmentIsOnTheBackStack() {
    val created = mutableListOf<TrackingViewModel>()
    val clerkFragment = ComposeFragment {
      ClerkViewModelStoreScope { created += viewModel<TrackingViewModel>() }
    }
    show(clerkFragment)
    val first = created.last()

    coverWithAnotherFragment()
    assertFalse(first.cleared)

    activity.supportFragmentManager.popBackStackImmediate()
    idleMainLooper()
    assertSame(first, created.last())
    assertFalse(first.cleared)

    activity.supportFragmentManager.beginTransaction().remove(clerkFragment).commitNow()
    idleMainLooper()
    assertTrue(first.cleared)
  }

  @Test
  fun componentInAFragmentUnderTheActivityStoreIsClearedWhenTheFragmentGoesToTheBackStack() {
    val created = mutableListOf<TrackingViewModel>()
    show(
      ComposeFragment {
        val activityOwner = checkNotNull(LocalActivity.current as? ViewModelStoreOwner)
        CompositionLocalProvider(LocalViewModelStoreOwner provides activityOwner) {
          ClerkViewModelStoreScope { created += viewModel<TrackingViewModel>() }
        }
      }
    )
    val first = created.last()

    coverWithAnotherFragment()

    assertTrue(first.cleared)
  }

  private fun show(fragment: Fragment) {
    activity.supportFragmentManager.beginTransaction().add(containerId, fragment).commitNow()
    idleMainLooper()
  }

  private fun coverWithAnotherFragment() {
    activity.supportFragmentManager
      .beginTransaction()
      .replace(containerId, ComposeFragment {})
      .addToBackStack(null)
      .commit()
    activity.supportFragmentManager.executePendingTransactions()
    idleMainLooper()
  }

  private fun idleMainLooper() {
    shadowOf(Looper.getMainLooper()).idle()
  }

  class ComposeFragment(private val content: @Composable () -> Unit = {}) : Fragment() {
    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
    ): View =
      ComposeView(inflater.context).apply {
        id = COMPOSE_VIEW_ID
        setContent(content)
      }
  }

  class TrackingViewModel : ViewModel() {
    var cleared = false
      private set

    override fun onCleared() {
      cleared = true
    }
  }

  private companion object {
    val COMPOSE_VIEW_ID = View.generateViewId()
  }
}
