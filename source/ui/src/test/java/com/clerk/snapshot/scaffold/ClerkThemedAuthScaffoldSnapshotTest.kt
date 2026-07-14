package com.clerk.snapshot.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.RectRulers
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.R
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import org.junit.Test

class ClerkThemedAuthScaffoldSnapshotTest : BaseSnapshotTest() {

  @Test
  fun authScaffoldAppliesOnlyTheStatusBarInsetThatOverlapsItsBounds() {
    val statusBarRulers = RectRulers()

    paparazzi.snapshot {
      Box(
        modifier =
          Modifier.size(width = 390.dp, height = 440.dp).layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(
              width = placeable.width,
              height = placeable.height,
              rulers = {
                statusBarRulers.left provides 0f
                statusBarRulers.top provides 48.dp.toPx()
                statusBarRulers.right provides coordinates.size.width.toFloat()
                statusBarRulers.bottom provides coordinates.size.height.toFloat()
              },
            ) {
              placeable.place(0, 0)
            }
          }
      ) {
        Column(Modifier.fillMaxSize().background(Color.LightGray)) {
          AuthScaffoldFixture(
            title = "Overlapping the status bar",
            modifier = Modifier.fillMaxWidth().height(200.dp),
            statusBarRulers = statusBarRulers,
          )
          Spacer(Modifier.height(40.dp))
          AuthScaffoldFixture(
            title = "Embedded below the status bar",
            modifier = Modifier.fillMaxWidth().height(200.dp),
            statusBarRulers = statusBarRulers,
          )
        }
      }
    }
  }
}

@Composable
private fun AuthScaffoldFixture(
  title: String,
  statusBarRulers: RectRulers,
  modifier: Modifier = Modifier,
) {
  ClerkThemedAuthScaffold(
    title = title,
    modifier = modifier,
    hasLogo = false,
    hasBackButton = false,
    showSignedInUserButton = false,
    statusBarRulers = statusBarRulers,
    trailingContent = {
      IconButton(onClick = {}) {
        Icon(
          modifier = Modifier.size(24.dp),
          painter = painterResource(R.drawable.ic_cross),
          contentDescription = "Close",
        )
      }
    },
  ) {}
}
