package com.clerk.quickstart.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignUpView(viewModel: SignUpViewModel = viewModel()) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  Column() {}
}
