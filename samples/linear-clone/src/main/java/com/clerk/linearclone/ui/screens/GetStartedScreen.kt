package com.clerk.linearclone.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clerk.linearclone.R
import com.clerk.linearclone.ui.theme.LinearCloneTheme
import com.clerk.linearclone.ui.theme.SecondaryGrey

@Composable
fun GetStartedScreen(onGetStartedClick: () -> Unit) {
  Box(
    modifier =
      Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primary).padding(32.dp)
  ) {
    Column(
      modifier = Modifier.padding(top = 100.dp).navigationBarsPadding().systemBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        modifier = Modifier.fillMaxWidth().size(300.dp),
        contentDescription = stringResource(R.string.logo),
        painter = painterResource(R.mipmap.ic_launcher_foreground),
      )

      Text(
        text = stringResource(R.string.welcome_to),
        color = SecondaryGrey,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
      )
      Text(
        text = stringResource(R.string.clerk),
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
      )
    }

    Button(
      onClick = onGetStartedClick,
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(48.dp),
      shape = RoundedCornerShape(8.dp),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.secondary,
          contentColor = Color.White,
        ),
    ) {
      Text(
        text = stringResource(R.string.get_started),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Preview
@Composable
private fun PreviewGetStartedScreen() {
  LinearCloneTheme { GetStartedScreen {} }
}
