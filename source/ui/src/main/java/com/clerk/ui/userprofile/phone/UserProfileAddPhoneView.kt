package com.clerk.ui.userprofile.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.input.ClerkPhoneNumberField
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.BottomSheetTopBar
import com.clerk.ui.userprofile.verify.Mode

@Composable
internal fun UserProfileAddPhoneView(
  onError: (String) -> Unit,
  onVerify: (Mode.Phone) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  UserProfileAddPhoneViewImpl(
    modifier = modifier,
    onError = onError,
    onVerify = onVerify,
    onDismiss = onDismiss,
  )
}

@Composable
private fun UserProfileAddPhoneViewImpl(
  onError: (String) -> Unit,
  onVerify: (Mode.Phone) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: UserProfileAddPhoneViewModel = viewModel(),
  onDismiss: () -> Unit,
) {
  var phoneNumber by rememberSaveable { mutableStateOf("") }
  val state by viewModel.state.collectAsStateWithLifecycle()
  val errorMessage = (state as? UserProfileAddPhoneViewModel.State.Error)?.message

  LaunchedEffect(state) {
    if (state is UserProfileAddPhoneViewModel.State.Error && errorMessage != null) {
      onError(errorMessage)
    }
    if (state is UserProfileAddPhoneViewModel.State.Success) {
      val phoneNumber = (state as UserProfileAddPhoneViewModel.State.Success).phoneNumber
      onVerify(Mode.Phone(phoneNumber))
    }
    viewModel.resetState()
  }

  Column(modifier = Modifier.fillMaxWidth().padding(bottom = dp24).then(modifier)) {
    BottomSheetTopBar(title = stringResource(R.string.add_phone_number), onClosePressed = onDismiss)
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = dp24).padding(vertical = dp24),
      verticalArrangement = Arrangement.spacedBy(dp24),
    ) {
      Text(
        text = stringResource(R.string.a_text_message_containing_a_verification_code_will_be_sent),
        style = ClerkMaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      ClerkPhoneNumberField(value = phoneNumber, onValueChange = { phoneNumber = it })
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
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {

  ClerkMaterialTheme {
    Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
      UserProfileAddPhoneViewImpl(onError = {}, onVerify = {}, onDismiss = {})
    }
  }
}
