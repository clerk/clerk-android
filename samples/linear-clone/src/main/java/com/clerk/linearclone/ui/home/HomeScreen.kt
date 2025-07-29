package com.clerk.linearclone.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.linearclone.R
import com.clerk.linearclone.ui.button.LinearCloneButton
import com.clerk.linearclone.ui.theme.PrimaryPurple
import com.clerk.linearclone.ui.theme.PrimaryWhite

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = viewModel()) {
  val state by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  when (state) {
    is HomeViewModel.UiState.SignedIn -> {
      HomeScreenContent(
        modifier = modifier,
        onSignOut = viewModel::signOut,
        onCreatePasskey = viewModel::createPasskey,
      )

      (state as HomeViewModel.UiState.SignedIn).passkeyResult?.let { result ->
        LaunchedEffect(key1 = result) {
          val message =
            when (result) {
              PasskeyResult.Failure -> context.getString(R.string.passkey_creation_failed)
              PasskeyResult.Success -> context.getString(R.string.passkey_created_successfully)
            }
          Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
}

@Composable
private fun HomeScreenContent(
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
  onCreatePasskey: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 42.dp).then(modifier),
    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    LinearCloneButton(
      backgroundColor = PrimaryPurple,
      onClick = onSignOut,
      buttonText = stringResource(R.string.sign_out),
      textColor = PrimaryWhite,
    )

    LinearCloneButton(
      buttonText = stringResource(R.string.create_a_passkey),
      textColor = PrimaryWhite,
      onClick = onCreatePasskey,
    )
  }
}
