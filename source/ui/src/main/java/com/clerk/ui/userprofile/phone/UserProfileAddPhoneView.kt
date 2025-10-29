package com.clerk.ui.userprofile.phone

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.input.ClerkPhoneNumberField
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState

@Composable
internal fun UserProfileAddPhoneView(modifier: Modifier = Modifier) {
  UserProfileAddPhoneViewImpl(modifier = modifier)
}

@Composable
private fun UserProfileAddPhoneViewImpl(
  modifier: Modifier = Modifier,
  viewModel: UserProfileAddPhoneViewModel = viewModel(),
) {
  val userProfileState = LocalUserProfileState.current
  var phoneNumber by rememberSaveable { mutableStateOf("") }
  val state by viewModel.state.collectAsStateWithLifecycle()
  val errorMessage = (state as? UserProfileAddPhoneViewModel.State.Error)?.message
  if (state is UserProfileAddPhoneViewModel.State.Success) {
    userProfileState.navigateBack()
  }

  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = stringResource(R.string.add_phone_number),
    errorMessage = errorMessage,
    content = {
      Text(
        text = stringResource(R.string.a_text_message_containing_a_verification_code_will_be_sent),
        style = ClerkMaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      Spacers.Vertical.Spacer24()
      ClerkPhoneNumberField(value = phoneNumber, onValueChange = { phoneNumber = it })
      Spacers.Vertical.Spacer24()
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.continue_text),
        onClick = { viewModel.addPhoneNumber(phoneNumber) },
        isLoading = state is UserProfileAddPhoneViewModel.State.Loading,
        icons =
          ClerkButtonDefaults.icons(
            trailingIcon = R.drawable.ic_triangle_right,
            trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
          ),
      )
    },
  )
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileAddPhoneViewImpl()
}
