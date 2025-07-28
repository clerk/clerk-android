package com.clerk.linearclone.ui.enteremail

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.linearclone.R
import com.clerk.linearclone.ui.button.LinearCloneButton
import com.clerk.linearclone.ui.theme.LinearCloneTheme
import com.clerk.linearclone.ui.theme.PrimaryGrey
import com.clerk.linearclone.ui.theme.PrimaryWhite
import com.clerk.linearclone.ui.theme.TertiaryGrey
import com.clerk.linearclone.ui.theme.TextBoxColor

@Composable
fun EnterEmailScreen(
  onNavigateToLogin: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: EnterEmailViewModel = viewModel(),
  onNavigateToEmailVerification: (String) -> Unit,
) {
  val state by viewModel.uiState.collectAsState()
  val context = LocalContext.current

  when (state) {
    EnterEmailViewModel.UiState.Error -> {
      Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
    }
    is EnterEmailViewModel.UiState.NeedsEmailCode ->
      onNavigateToEmailVerification((state as EnterEmailViewModel.UiState.NeedsEmailCode).email)
    EnterEmailViewModel.UiState.SignedOut ->
      EnterEmailContent(
        modifier = modifier,
        viewModel = viewModel,
        onNavigateToLogin = onNavigateToLogin,
      )
  }
}

@Composable
private fun EnterEmailContent(
  viewModel: EnterEmailViewModel,
  modifier: Modifier = Modifier,
  onNavigateToLogin: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(color = MaterialTheme.colorScheme.primary)
        .padding(horizontal = 60.dp)
        .imePadding()
        .then(modifier),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
  ) {
    Image(
      modifier = Modifier.fillMaxWidth().size(100.dp),
      contentDescription = stringResource(R.string.logo),
      painter = painterResource(R.mipmap.ic_launcher_foreground),
    )

    Text(
      text = stringResource(R.string.what_s_your_email_address),
      color = PrimaryWhite,
      fontSize = 20.sp,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
    )

    InputContent(
      buttonText = stringResource(R.string.continue_with_email),
      placeholder = stringResource(R.string.enter_your_email_address),
      onClick = viewModel::prepareEmailVerification,
      navigateToLogin = onNavigateToLogin,
      contentTypeValue = ContentType.EmailAddress,
    )
  }
}

@Composable
fun InputContent(
  buttonText: String,
  placeholder: String,
  onClick: (String) -> Unit,
  contentTypeValue: ContentType,
  modifier: Modifier = Modifier,
  buttonColor: Color = PrimaryGrey,
  navigateToLogin: () -> Unit,
) {
  var value by remember { mutableStateOf("") }
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
  ) {
    OutlinedTextField(
      colors =
        OutlinedTextFieldDefaults.colors(
          unfocusedContainerColor = TextBoxColor,
          focusedContainerColor = TextBoxColor,
          unfocusedBorderColor = PrimaryGrey,
          focusedBorderColor = MaterialTheme.colorScheme.secondary,
        ),
      modifier =
        Modifier.fillMaxWidth().then(modifier).semantics { contentType = contentTypeValue },
      value = value,
      onValueChange = { value = it },
      placeholder = { Text(fontSize = 14.sp, text = placeholder, color = TertiaryGrey) },
    )

    LinearCloneButton(
      backgroundColor = buttonColor,
      onClick = { onClick(value) },
      buttonText = buttonText,
      textColor = PrimaryWhite,
    )

    TextButton(
      colors = ButtonDefaults.textButtonColors(contentColor = PrimaryWhite),
      onClick = navigateToLogin,
    ) {
      Text("Back to login")
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewEmailEntryScreen() {
  LinearCloneTheme() {
    EnterEmailScreen(onNavigateToLogin = {}, onNavigateToEmailVerification = {})
  }
}
