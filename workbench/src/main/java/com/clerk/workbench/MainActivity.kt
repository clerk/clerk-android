package com.clerk.workbench

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.workbench.ui.theme.ClerkPrimary
import com.clerk.workbench.ui.theme.WorkbenchTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      WorkbenchTheme {
        MainContent(
          onSave = { StorageHelper.saveValue(StorageKey.PUBLIC_KEY, it) },
          onClear = { StorageHelper.deleteValue(StorageKey.PUBLIC_KEY) },
          onClickFirstItem = {},
          onClickSecondItem = { context.startActivity(Intent(context, UiActivity::class.java)) },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
  onClear: () -> Unit,
  onSave: (String) -> Unit,
  onClickFirstItem: () -> Unit,
  onClickSecondItem: () -> Unit,
) {
  var showBottomSheet by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showBottomSheet = true },
        containerColor = ClerkPrimary,
        contentColor = Color.White,
      ) {
        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
      }
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .background(color = MaterialTheme.colorScheme.background)
          .padding(horizontal = Spacing.small)
    ) {
      AppHeader()
      Spacer(modifier = Modifier.height(Spacing.large))
      InstructionsCard()
      Spacer(modifier = Modifier.height(Spacing.large))
      TestOptionsCard(onClickFirstItem = onClickFirstItem, onClickSecondItem = onClickSecondItem)
    }
  }

  if (showBottomSheet) {
    ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
      SettingsBottomSheetContent(
        onSave = { publishableKey ->
          showBottomSheet = false
          onSave(publishableKey)
        },
        onClear = onClear,
      )
    }
  }
}

@Composable
private fun AppHeader() {
  Text(
    modifier = Modifier.padding(top = Spacing.extraLarge),
    text = WorkbenchConstants.APP_TITLE,
    color = MaterialTheme.colorScheme.onBackground,
    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
  )
  Spacer(modifier = Modifier.height(Spacing.extraSmall))
  Text(
    text = WorkbenchConstants.INSTRUCTIONS_TITLE,
    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
    color = MaterialTheme.colorScheme.onBackground,
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
          color = MaterialTheme.colorScheme.onSurface,
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
    Column(modifier = Modifier.padding(Spacing.small)) {
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
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
  ) {
    content()
  }
}

@Composable
private fun WorkbenchDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(vertical = Spacing.dividerVerticalPadding),
    thickness = Spacing.dividerThickness,
    color = MaterialTheme.colorScheme.outlineVariant,
  )
}

@Composable
private fun ClickableTestItem(text: String, onClick: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(8.dp)) {
    Text(text = text, color = ClerkPrimary, style = MaterialTheme.typography.titleMedium)
  }
}

@Composable
private fun SettingsBottomSheetContent(onSave: (String) -> Unit, onClear: () -> Unit) {
  val publicKey = StorageHelper.loadValue(StorageKey.PUBLIC_KEY) ?: ""
  var publishableKey by remember { mutableStateOf(publicKey) }

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(Spacing.medium)
        .padding(bottom = Spacing.large) // Extra padding for bottom sheet
  ) {
    Text(
      text = WorkbenchConstants.SETTINGS_TITLE,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(Spacing.small))
    Text(
      text = WorkbenchConstants.SETTINGS_DESCRIPTION,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(Spacing.medium))
    OutlinedTextField(
      value = publishableKey,
      onValueChange = { publishableKey = it },
      label = { Text(WorkbenchConstants.PUBLISHABLE_KEY_LABEL) },
      placeholder = { Text(WorkbenchConstants.PUBLISHABLE_KEY_PLACEHOLDER) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    Spacer(modifier = Modifier.height(Spacing.medium))
    Button(onClick = { onSave(publishableKey) }, modifier = Modifier.fillMaxWidth()) {
      Text(WorkbenchConstants.SAVE_BUTTON_TEXT)
    }
    Button(
      modifier = Modifier.fillMaxWidth(),
      onClick = {
        publishableKey = ""
        onClear()
      },
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
    ) {
      Text("Clear")
    }
  }
}

// Constants
private object WorkbenchConstants {
  const val APP_TITLE = "Clerk Workbench"
  const val INSTRUCTIONS_TITLE = "Instructions:"
  const val SETTINGS_TITLE = "Settings"
  const val SETTINGS_DESCRIPTION = "Please enter your publishable key"
  const val PUBLISHABLE_KEY_LABEL = "Publishable Key"
  const val PUBLISHABLE_KEY_PLACEHOLDER = "Enter publishable key"
  const val SAVE_BUTTON_TEXT = "Save"

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
  WorkbenchTheme {
    MainContent(onSave = {}, onClear = {}, onClickFirstItem = {}, onClickSecondItem = {})
  }
}

@PreviewLightDark
@Composable
private fun PreviewSettingsBottomSheet() {
  WorkbenchTheme { SettingsBottomSheetContent(onClear = {}, onSave = {}) }
}
