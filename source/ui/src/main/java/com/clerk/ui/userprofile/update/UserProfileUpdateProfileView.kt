package com.clerk.ui.userprofile.update

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState
import java.io.File
import java.io.FileOutputStream

@Composable
internal fun UserProfileUpdateProfileView(modifier: Modifier = Modifier) {
  UserProfileUpdateProfileViewImpl(modifier = modifier)
}

@Composable
private fun UserProfileUpdateProfileViewImpl(
  modifier: Modifier = Modifier,
  viewModel: UpdateProfileViewModel = viewModel(),
) {
  val userProfileState = LocalUserProfileState.current
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  val state by viewModel.state.collectAsState()
  val errorMessage = (state as? UpdateProfileViewModel.State.Error)?.message
  LaunchedEffect(state) {
    if (state is UpdateProfileViewModel.State.Success) {
      userProfileState.navigateBack()
      viewModel.reset()
    }
  }

  val context = LocalContext.current

  val (takePhotoLauncher, pickPhotoLauncher) = createLaunchers(context, viewModel)

  ClerkMaterialTheme {
    ClerkThemedProfileScaffold(
      modifier = modifier,
      title = stringResource(R.string.edit_profile),
      hasBackButton = true,
      horizontalPadding = dp0,
      onBackPressed = { userProfileState.navigateBack() },
      errorMessage = errorMessage,
      content = {
        Box(modifier = Modifier.fillMaxWidth()) {
          AvatarView(
            hasEditButton = true,
            modifier = Modifier.align(Alignment.Center),
            size = AvatarSize.X_LARGE,
            shape = CircleShape,
            avatarType = AvatarType.USER,
            imageUrl = user?.imageUrl,
            onEditTakePhoto = { takePhotoLauncher.launch(null) },
            onEditChoosePhoto = {
              pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            onEditRemovePhoto = { viewModel.removeProfileImage() },
          )
        }
        Spacers.Vertical.Spacer32()
        ProfileFields(
          isLoading = state is UpdateProfileViewModel.State.Loading,
          viewModel = viewModel,
        )
      },
    )
  }
}

@Composable
private fun createLaunchers(
  context: Context,
  viewModel: UpdateProfileViewModel,
): Pair<
  ManagedActivityResultLauncher<Void?, Bitmap?>,
  ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
> {
  val takePhotoLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
      if (bitmap != null) {
        val file = createImageFileFromBitmap(context.cacheDir.absolutePath, bitmap)
        viewModel.uploadProfileImage(file)
      }
    }

  val pickPhotoLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val file = createImageFileFromUri(context, uri)
        viewModel.uploadProfileImage(file)
      }
    }
  return Pair(takePhotoLauncher, pickPhotoLauncher)
}

@Composable
private fun ProfileFields(isLoading: Boolean, viewModel: UpdateProfileViewModel) {
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  var username by rememberSaveable { mutableStateOf(user?.username) }
  var firstName by rememberSaveable { mutableStateOf(user?.firstName) }
  var lastName by rememberSaveable { mutableStateOf(user?.lastName) }
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = dp18),
    verticalArrangement = Arrangement.spacedBy(dp24),
  ) {
    if (Clerk.isUserNameEnabled) {
      ClerkTextField(
        value = username.orEmpty(),
        onValueChange = { username = it },
        label = stringResource(R.string.username),
      )
    }

    if (Clerk.isFirstNameEnabled) {
      ClerkTextField(
        value = firstName.orEmpty(),
        onValueChange = { firstName = it },
        label = stringResource(R.string.first_name),
      )
    }
    if (Clerk.isLastNameEnabled) {
      ClerkTextField(
        value = lastName.orEmpty(),
        onValueChange = { lastName = it },
        label = stringResource(R.string.last_name),
      )
    }
    if (Clerk.isFirstNameEnabled || Clerk.isLastNameEnabled || Clerk.isUserNameEnabled) {
      ClerkButton(
        isEnabled = true,
        isLoading = isLoading,
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.save),
        onClick = {
          viewModel.save(firstName = firstName, lastName = lastName, username = username)
        },
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileUpdateProfileViewImpl()
}

private const val IMAGE_COMPRESSION = 100

private fun createImageFileFromBitmap(directory: String, bitmap: Bitmap): File {
  val file = File(directory, "image.png")
  val fileOutputStream = FileOutputStream(file)
  bitmap.compress(CompressFormat.PNG, IMAGE_COMPRESSION, fileOutputStream)
  fileOutputStream.close()
  return file
}

private fun createImageFileFromUri(context: android.content.Context, uri: android.net.Uri): File {
  val file = File(context.cacheDir, "image.png")
  val inputStream = context.contentResolver.openInputStream(uri)
  val fileOutputStream = FileOutputStream(file)
  inputStream!!.copyTo(fileOutputStream)
  fileOutputStream.close()
  inputStream.close()
  return file
}
