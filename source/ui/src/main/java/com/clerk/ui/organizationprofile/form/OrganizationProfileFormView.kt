@file:Suppress("LongParameterList")

package com.clerk.ui.organizationprofile.form

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import coil3.compose.SubcomposeAsyncImage
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp14
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.dimens.dp96
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.sessiontask.organization.createOrganizationSlug
import com.clerk.ui.sessiontask.organization.isValidOrganizationSlug
import com.clerk.ui.theme.ClerkMaterialTheme
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
@Suppress("CyclomaticComplexMethod", "LongMethod")
internal fun OrganizationProfileFormView(
  initialName: String,
  initialSlug: String?,
  slugEnabled: Boolean,
  submitText: String,
  isLoading: Boolean,
  onSubmit: (OrganizationProfileFormSubmit) -> Unit,
  modifier: Modifier = Modifier,
  initialLogoUrl: String? = null,
  initialHasLogo: Boolean = false,
  preloadInitialLogo: Boolean = false,
  autoGenerateSlug: Boolean = false,
  useAvatarLogoUpload: Boolean = false,
  enabled: Boolean = true,
  advisory: (@Composable () -> Unit)? = null,
  footer: (@Composable () -> Unit)? = null,
) {
  val context = LocalContext.current
  val invalidSlugMessage = stringResource(R.string.enter_a_valid_organization_slug)
  var organizationName by rememberSaveable(initialName) { mutableStateOf(initialName) }
  var slug by rememberSaveable(initialSlug) { mutableStateOf(initialSlug.orEmpty()) }
  var slugError by rememberSaveable { mutableStateOf<String?>(null) }
  var selectedLogoFile by remember { mutableStateOf<File?>(null) }
  var preloadedLogoFile by remember { mutableStateOf<File?>(null) }
  var removeLogo by rememberSaveable(initialLogoUrl, initialHasLogo) { mutableStateOf(false) }
  var logoIsLoading by remember { mutableStateOf(false) }

  val imagePicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        selectedLogoFile = createImageFileFromUri(context, uri)
        removeLogo = false
      }
    }

  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
      if (bitmap != null) {
        selectedLogoFile = createImageFileFromBitmap(context, bitmap)
        removeLogo = false
      }
    }

  LaunchedEffect(initialLogoUrl, preloadInitialLogo) {
    if (preloadInitialLogo && !initialLogoUrl.isNullOrBlank() && selectedLogoFile == null) {
      logoIsLoading = true
      preloadedLogoFile = loadDefaultLogoFile(context, initialLogoUrl)
      logoIsLoading = false
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(dp24),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    advisory?.invoke()

    val logoModel =
      if (removeLogo) {
        null
      } else {
        selectedLogoFile ?: preloadedLogoFile ?: initialLogoUrl?.takeIf { it.isNotBlank() }
      }
    val canRemoveLogo = selectedLogoFile != null || initialHasLogo
    val uploadEnabled = enabled && !isLoading
    val onUploadLogo = {
      imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val onRemoveLogo = {
      selectedLogoFile = null
      preloadedLogoFile = null
      removeLogo = initialHasLogo
    }
    if (useAvatarLogoUpload) {
      OrganizationAvatarLogoSection(
        logoModel = logoModel,
        isLoading = logoIsLoading,
        canRemoveLogo = canRemoveLogo,
        enabled = uploadEnabled,
        onTakePhotoClick = { cameraLauncher.launch(null) },
        onUploadClick = onUploadLogo,
        onRemoveClick = onRemoveLogo,
      )
    } else {
      OrganizationLogoSection(
        logoModel = logoModel,
        isLoading = logoIsLoading,
        canRemoveLogo = canRemoveLogo,
        enabled = uploadEnabled,
        onUploadClick = onUploadLogo,
        onRemoveClick = onRemoveLogo,
      )
    }

    OrganizationFormFields(
      organizationName = organizationName,
      slug = slug,
      slugEnabled = slugEnabled,
      slugError = slugError,
      enabled = enabled && !isLoading,
      onOrganizationNameChange = {
        organizationName = it
        if (autoGenerateSlug) slug = createOrganizationSlug(it)
        slugError = null
      },
      onSlugChange = {
        slug = it
        slugError = null
      },
    )

    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = submitText,
      isLoading = isLoading,
      isEnabled = enabled && organizationName.trim().isNotEmpty() && !isLoading && !logoIsLoading,
      onClick = {
        val name = organizationName.trim()
        val trimmedSlug = slug.trim()
        if (slugEnabled && !isValidOrganizationSlug(trimmedSlug)) {
          slugError = invalidSlugMessage
          return@ClerkButton
        }
        onSubmit(
          OrganizationProfileFormSubmit(
            name = name,
            slug = if (slugEnabled) trimmedSlug else null,
            logoFile = selectedLogoFile ?: preloadedLogoFile,
            removeLogo = removeLogo,
          )
        )
      },
    )

    footer?.invoke()
  }
}

internal data class OrganizationProfileFormSubmit(
  val name: String,
  val slug: String?,
  val logoFile: File?,
  val removeLogo: Boolean,
)

@Composable
private fun OrganizationAvatarLogoSection(
  logoModel: Any?,
  isLoading: Boolean,
  canRemoveLogo: Boolean,
  enabled: Boolean,
  onTakePhotoClick: () -> Unit,
  onUploadClick: () -> Unit,
  onRemoveClick: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Surface(
      modifier = Modifier.align(Alignment.Center).size(dp96),
      shape = CircleShape,
      color = ClerkMaterialTheme.colors.muted,
      border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
    ) {}
    AvatarView(
      modifier = Modifier.align(Alignment.Center),
      imageUrl = null,
      imageModel = logoModel,
      size = AvatarSize.X_LARGE,
      shape = CircleShape,
      avatarType = AvatarType.ORGANIZATION,
      hasEditButton = enabled,
      editContentDescription = R.string.upload_logo,
      choosePhotoText = R.string.upload_logo,
      removePhotoText = R.string.remove_logo,
      showRemovePhoto = canRemoveLogo,
      showPlaceholder = logoModel != null,
      onEditTakePhoto = onTakePhotoClick,
      onEditChoosePhoto = onUploadClick,
      onEditRemovePhoto = onRemoveClick,
    )
    if (isLoading) {
      Surface(
        modifier = Modifier.align(Alignment.Center).size(dp96),
        shape = CircleShape,
        color = ClerkMaterialTheme.colors.shadow.copy(alpha = 0.25f),
      ) {
        Box(contentAlignment = Alignment.Center) {
          CircularProgressIndicator(modifier = Modifier.size(dp24))
        }
      }
    }
  }
}

@Composable
private fun OrganizationLogoSection(
  logoModel: Any?,
  isLoading: Boolean,
  canRemoveLogo: Boolean,
  enabled: Boolean,
  onUploadClick: () -> Unit,
  onRemoveClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(dp16),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LogoPreview(logoModel = logoModel, isLoading = isLoading)
    Column(verticalArrangement = Arrangement.spacedBy(dp12)) {
      PillActionButton(
        text = stringResource(R.string.upload_logo),
        isEnabled = enabled,
        onClick = onUploadClick,
      )
      if (canRemoveLogo) {
        PillActionButton(
          text = stringResource(R.string.remove_logo),
          isEnabled = enabled,
          onClick = onRemoveClick,
        )
      }
      Text(
        text = stringResource(R.string.recommended_logo_size),
        style = ClerkMaterialTheme.typography.bodySmall,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
    }
  }
}

@Composable
private fun LogoPreview(logoModel: Any?, isLoading: Boolean) {
  Box(modifier = Modifier.size(dp96).clip(CircleShape), contentAlignment = Alignment.Center) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      shape = CircleShape,
      color = ClerkMaterialTheme.colors.muted,
      border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.border),
    ) {
      if (logoModel != null) {
        SubcomposeAsyncImage(
          model = logoModel,
          contentDescription = stringResource(R.string.logo),
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
          loading = { CircularProgressIndicator(modifier = Modifier.size(dp24)) },
          error = { OrganizationPlaceholderIcon() },
        )
      } else {
        OrganizationPlaceholderIcon()
      }
    }
    if (isLoading) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = ClerkMaterialTheme.colors.shadow.copy(alpha = 0.25f),
      ) {
        Box(contentAlignment = Alignment.Center) {
          CircularProgressIndicator(modifier = Modifier.size(dp24))
        }
      }
    }
  }
}

@Composable
private fun OrganizationPlaceholderIcon() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Icon(
      modifier = Modifier.size(dp48),
      painter = painterResource(R.drawable.ic_organization),
      contentDescription = null,
      tint = ClerkMaterialTheme.colors.mutedForeground,
    )
  }
}

@Composable
private fun OrganizationFormFields(
  organizationName: String,
  slug: String,
  slugEnabled: Boolean,
  slugError: String?,
  enabled: Boolean,
  onOrganizationNameChange: (String) -> Unit,
  onSlugChange: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dp16)) {
    ClerkTextField(
      value = organizationName,
      onValueChange = onOrganizationNameChange,
      label = stringResource(R.string.organization_name),
      enabled = enabled,
    )
    if (slugEnabled) {
      ClerkTextField(
        value = slug,
        onValueChange = onSlugChange,
        label = stringResource(R.string.slug),
        enabled = enabled,
        isError = slugError != null,
        supportingText = slugError,
        keyboardOptions =
          KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
          ),
      )
    }
  }
}

@Composable
private fun PillActionButton(text: String, isEnabled: Boolean, onClick: () -> Unit) {
  Surface(
    modifier = Modifier.clickable(enabled = isEnabled, onClick = onClick),
    shape = ClerkMaterialTheme.shape,
    color = ClerkMaterialTheme.colors.background,
    border = BorderStroke(dp1, ClerkMaterialTheme.computedColors.buttonBorder),
    shadowElevation = dp2,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = dp14, vertical = dp8),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp8),
    ) {
      Text(
        text = text,
        style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
        color =
          if (isEnabled) {
            ClerkMaterialTheme.colors.foreground
          } else {
            ClerkMaterialTheme.colors.mutedForeground
          },
      )
    }
  }
}

private suspend fun loadDefaultLogoFile(context: Context, url: String): File? {
  return withContext(Dispatchers.IO) {
    runCatching {
        URL(url).openStream().use { input ->
          val file = File(context.cacheDir, "organization_default_logo_${System.nanoTime()}.jpg")
          FileOutputStream(file).use { output -> input.copyTo(output) }
          file
        }
      }
      .getOrNull()
  }
}

private fun createImageFileFromUri(context: Context, uri: Uri): File? {
  return runCatching {
      val file = File(context.cacheDir, "organization_logo_${System.nanoTime()}.jpg")
      context.contentResolver.openInputStream(uri).use { input ->
        FileOutputStream(file).use { output -> requireNotNull(input).copyTo(output) }
      }
      file
    }
    .getOrNull()
}

private const val IMAGE_COMPRESSION = 100

private fun createImageFileFromBitmap(context: Context, bitmap: Bitmap): File? {
  return runCatching {
      val file = File(context.cacheDir, "organization_logo_${System.nanoTime()}.png")
      FileOutputStream(file).use { output ->
        bitmap.compress(CompressFormat.PNG, IMAGE_COMPRESSION, output)
      }
      file
    }
    .getOrNull()
}
