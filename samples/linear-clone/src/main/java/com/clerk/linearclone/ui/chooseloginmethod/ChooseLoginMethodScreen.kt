package com.clerk.linearclone.ui.chooseloginmethod

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.clerk.linearclone.ui.theme.PrimaryPurple
import com.clerk.linearclone.ui.theme.PrimaryWhite

@Composable
fun ChooseLoginMethodScreen(
  modifier: Modifier = Modifier,
  viewModel: ChooseLoginViewModel = viewModel(),
  onClickUseEmail: () -> Unit,
) {

  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(color = MaterialTheme.colorScheme.primary)
        .padding(horizontal = 50.dp)
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
      text = stringResource(R.string.add_an_account),
      color = PrimaryWhite,
      fontSize = 20.sp,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    LinearCloneButton(
      backgroundColor = PrimaryPurple,
      onClick = { viewModel.authWithGoogle() },
      buttonText = stringResource(R.string.continue_with_google),
      textColor = PrimaryWhite,
      leadingIcon = R.drawable.ic_google,
    )

    LinearCloneButton(
      backgroundColor = PrimaryGrey,
      onClick = onClickUseEmail,
      buttonText = stringResource(R.string.continue_with_email),
      textColor = PrimaryWhite,
    )
    LinearCloneButton(
      backgroundColor = PrimaryGrey,
      onClick = {},
      buttonText = stringResource(R.string.continue_with_passkey),
      textColor = PrimaryWhite,
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewLoginScreen() {
  LinearCloneTheme { ChooseLoginMethodScreen(onClickUseEmail = {}) }
}
