package com.clerk.linearclone.ui.button

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.linearclone.R
import com.clerk.linearclone.ui.theme.PrimaryGrey
import com.clerk.linearclone.ui.theme.PrimaryPurple
import com.clerk.linearclone.ui.theme.PrimaryWhite

@Composable
fun LinearCloneButton(
  backgroundColor: Color,
  onClick: () -> Unit,
  buttonText: String,
  textColor: Color,
  modifier: Modifier = Modifier,
  @DrawableRes leadingIcon: Int? = null,
) {
  Button(
    colors =
      ButtonDefaults.buttonColors(containerColor = backgroundColor, contentColor = textColor),
    modifier = Modifier.fillMaxWidth().height(52.dp).then(modifier),
    onClick = onClick,
    shape = RoundedCornerShape(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      leadingIcon?.let {
        Image(
          modifier = Modifier.size(18.dp),
          painter = painterResource(it),
          contentDescription = null,
        )
      }
      Text(text = buttonText)
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewLinearCloneButtonWithIcon() {
  LinearCloneButton(
    backgroundColor = PrimaryPurple,
    onClick = {},
    buttonText = "Continue with Google",
    textColor = PrimaryWhite,
    leadingIcon = R.drawable.ic_google,
  )
}

@PreviewLightDark
@Composable
private fun PreviewLinearCloneButtonWithNoIcon() {
  LinearCloneButton(
    backgroundColor = PrimaryGrey,
    onClick = {},
    buttonText = "Continue with email",
    textColor = PrimaryWhite,
  )
}
