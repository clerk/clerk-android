package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.workbench.ui.theme.ClerkTheme
import com.clerk.workbench.ui.theme.WorkbenchBackground
import com.clerk.workbench.ui.theme.WorkbenchCardBackground
import com.clerk.workbench.ui.theme.WorkbenchDivider
import com.clerk.workbench.ui.theme.WorkbenchPrimary
import com.clerk.workbench.ui.theme.WorkbenchSecondaryText

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { ClerkTheme { MainContent() } }
  }
}

@Composable
private fun MainContent() {
  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .background(color = WorkbenchBackground)
          .padding(horizontal = Spacing.small)
    ) {
      AppHeader()
      Spacer(modifier = Modifier.height(Spacing.large))
      InstructionsCard()
      Spacer(modifier = Modifier.height(Spacing.large))
      TestOptionsCard(onClickFirstItem = {}, onClickSecondItem = {})
    }
  }
}

@Composable
private fun AppHeader() {
  Text(
    modifier = Modifier.padding(top = Spacing.extraLarge),
    text = WorkbenchConstants.APP_TITLE,
    color = Color.Black,
    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
  )
  Spacer(modifier = Modifier.height(Spacing.extraSmall))
  Text(
    text = WorkbenchConstants.INSTRUCTIONS_TITLE,
    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
    color = WorkbenchSecondaryText,
  )
  Spacer(modifier = Modifier.height(Spacing.extraSmall))
}

@Composable
private fun InstructionsCard() {
  WorkbenchCard {
    Column(modifier = Modifier.padding(Spacing.medium)) {
      WorkbenchConstants.instructionSteps.forEachIndexed { index, step ->
        if (index > 0) {
          WorkbenchDivider()
        }
        Text(
          text = step,
          color = Color.Black,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Normal,
        )
      }
    }
  }
}

@Composable
private fun TestOptionsCard(onClickFirstItem: () -> Unit, onClickSecondItem: () -> Unit) {
  WorkbenchCard {
    Column(modifier = Modifier.padding(Spacing.medium)) {
      ClickableTestItem(text = "Test 1", onClickFirstItem)
      WorkbenchDivider()
      ClickableTestItem(text = "Test 2", onClick = onClickSecondItem)
    }
  }
}

@Composable
private fun WorkbenchCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(Spacing.cardCornerRadius),
    colors = CardDefaults.cardColors(containerColor = WorkbenchCardBackground),
  ) {
    content()
  }
}

@Composable
private fun WorkbenchDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(vertical = Spacing.dividerVerticalPadding),
    thickness = Spacing.dividerThickness,
    color = WorkbenchDivider,
  )
}

@Composable
private fun ClickableTestItem(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Text(
    modifier = modifier.clickable { onClick() },
    text = text,
    color = WorkbenchPrimary,
    style = MaterialTheme.typography.titleMedium,
  )
}

// Constants
private object WorkbenchConstants {
  const val APP_TITLE = "Clerk Workbench"
  const val INSTRUCTIONS_TITLE = "Instructions:"

  val instructionSteps =
    listOf(
      "Create an account on Clerk.com",
      "Create an app",
      "Copy your publishable key from the dashboard under the API keys section.",
      "Paste your publishable key in the setting of this app and tap Save",
    )
}

// Dimensions
private object Spacing {
  val extraSmall = 6.dp
  val small = 12.dp
  val medium = 18.dp
  val large = 36.dp
  val extraLarge = 56.dp
  val dividerThickness = 1.dp
  val cardCornerRadius = 8.dp
  val dividerVerticalPadding = 10.dp
}

@PreviewLightDark
@Composable
private fun MainContentPreview() {
  ClerkTheme { MainContent() }
}
