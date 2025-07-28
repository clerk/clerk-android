package com.clerk.linearclone.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clerk.linearclone.R
import com.clerk.linearclone.ui.theme.LinearCloneTheme
import com.clerk.linearclone.ui.theme.PrimaryWhite
import com.clerk.linearclone.ui.theme.SecondaryGrey

@Composable
fun EmailVerificationScreen(
  email: String,
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
      text = stringResource(R.string.check_your_email),
      color = PrimaryWhite,
      fontSize = 20.sp,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
    )

    Text(
      text =
        buildAnnotatedString {
          append(
            stringResource(R.string.we_ve_sent_you_a_temporary_login_code_please_check_you_inbox_at)
          )
          withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
            append(email)
          }
        },
      color = SecondaryGrey,
      fontSize = 12.sp,
      style = TextStyle(lineHeight = 14.sp),
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
    )

    InputContent(
      contentTypeValue = ContentType.SmsOtpCode,
      buttonText = stringResource(R.string.continue_with_email),
      placeholder = stringResource(R.string.enter_your_email_address),
      navigateToLogin = onNavigateToLogin,
      onClick = {},
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewEmailEntryScreen() {
  LinearCloneTheme { EmailVerificationScreen(onNavigateToLogin = {}, email = "sam@clerk.dev") }
}
