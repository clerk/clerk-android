package com.clerk.ui.sessiontask.organization

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.clerk.api.Clerk
import com.clerk.api.organizations.OrganizationCreationDefaults
import com.clerk.ui.R
import com.clerk.ui.auth.handleSessionTaskCompletion
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.dimens.dp96
import com.clerk.ui.core.footer.SecuredByClerkView
import com.clerk.ui.core.header.HeaderTextView
import com.clerk.ui.core.header.HeaderType
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
@Suppress("LongMethod")
internal fun SessionTaskCreateOrganizationView(
  creationDefaults: OrganizationCreationDefaults?,
  showBackButton: Boolean,
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SessionTaskCreateOrganizationViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val authState = LocalAuthState.current
  val context = LocalContext.current
  val slugEnabled = Clerk.organizationSlugIsEnabled
  val defaultName = creationDefaults?.form?.name.orEmpty()
  val defaultSlug = creationDefaults?.form?.slug ?: createOrganizationSlug(defaultName)
  val defaultLogoUrl = creationDefaults?.form?.logo

  var organizationName by rememberSaveable(creationDefaults) { mutableStateOf(defaultName) }
  var slug by rememberSaveable(creationDefaults) { mutableStateOf(defaultSlug) }
  var slugError by rememberSaveable { mutableStateOf<String?>(null) }
  var selectedLogoFile by remember { mutableStateOf<File?>(null) }
  var preloadedLogoFile by remember { mutableStateOf<File?>(null) }
  var logoIsLoading by remember { mutableStateOf(false) }

  val imagePicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        selectedLogoFile = createImageFileFromUri(context, uri)
      }
    }

  LaunchedEffect(defaultLogoUrl) {
    if (!defaultLogoUrl.isNullOrBlank() && selectedLogoFile == null) {
      logoIsLoading = true
      preloadedLogoFile = loadDefaultLogoFile(context, defaultLogoUrl)
      logoIsLoading = false
    }
  }

  LaunchedEffect(state.completedSession) {
    state.completedSession?.let {
      authState.handleSessionTaskCompletion(it, onAuthComplete)
      viewModel.clearCompletedSession()
    }
  }

  SessionTaskOrganizationScaffold(
    modifier = modifier,
    errorMessage = state.errorMessage,
    onErrorShown = viewModel::clearError,
    hasBackButton = showBackButton,
    onBackPressed = { authState.navigateBack() },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(horizontal = dp18, vertical = dp16),
      verticalArrangement = Arrangement.spacedBy(dp24),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      item { CreateOrganizationHeader() }

      creationDefaults?.advisory?.let { advisory -> item { AdvisoryText(advisory = advisory) } }

      item {
        OrganizationLogoSection(
          logoModel = selectedLogoFile ?: preloadedLogoFile ?: defaultLogoUrl,
          isLoading = logoIsLoading,
          onUploadClick = {
            imagePicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
        )
      }

      item {
        OrganizationFormFields(
          organizationName = organizationName,
          slug = slug,
          slugEnabled = slugEnabled,
          slugError = slugError,
          onOrganizationNameChange = {
            organizationName = it
            slug = createOrganizationSlug(it)
            slugError = null
          },
          onSlugChange = {
            slug = it
            slugError = null
          },
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = stringResource(R.string.continue_text),
          isLoading = state.isLoading,
          isEnabled = organizationName.trim().isNotEmpty() && !state.isLoading,
          onClick = {
            val name = organizationName.trim()
            val trimmedSlug = slug.trim()
            if (slugEnabled && !isValidOrganizationSlug(trimmedSlug)) {
              slugError = context.getString(R.string.enter_a_valid_organization_slug)
              return@ClerkButton
            }
            viewModel.createOrganization(
              name = name,
              slug = if (slugEnabled) trimmedSlug else null,
              logoFile = selectedLogoFile ?: preloadedLogoFile,
            )
          },
        )
      }

      item { SecuredByClerkView() }
    }
  }
}

@Composable
private fun CreateOrganizationHeader() {
  Column(
    modifier = Modifier.fillMaxWidth().padding(bottom = dp8),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(dp8),
  ) {
    HeaderTextView(text = stringResource(R.string.create_organization), type = HeaderType.Title)
    HeaderTextView(
      text = stringResource(R.string.enter_your_organization_details_to_continue),
      type = HeaderType.Subtitle,
    )
  }
}

@Composable
private fun AdvisoryText(advisory: OrganizationCreationDefaults.Advisory) {
  val message =
    when (advisory.code) {
      "organization_already_exists" ->
        stringResource(
          R.string.organization_already_exists_advisory,
          advisory.meta["organization_name"].orEmpty(),
          advisory.meta["organization_domain"].orEmpty(),
        )
      else -> null
    }

  message?.let {
    Text(
      modifier = Modifier.fillMaxWidth(),
      text = it,
      style = ClerkMaterialTheme.typography.bodySmall,
      color = ClerkMaterialTheme.colors.warning,
    )
  }
}

@Composable
private fun OrganizationLogoSection(
  logoModel: Any?,
  isLoading: Boolean,
  onUploadClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(dp16),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LogoPreview(logoModel = logoModel, isLoading = isLoading)
    Column(verticalArrangement = Arrangement.spacedBy(dp12)) {
      PillActionButton(text = stringResource(R.string.upload_logo), onClick = onUploadClick)
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
@Suppress("LongParameterList")
private fun OrganizationFormFields(
  organizationName: String,
  slug: String,
  slugEnabled: Boolean,
  slugError: String?,
  onOrganizationNameChange: (String) -> Unit,
  onSlugChange: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dp16)) {
    ClerkTextField(
      value = organizationName,
      onValueChange = onOrganizationNameChange,
      label = stringResource(R.string.organization_name),
    )
    if (slugEnabled) {
      ClerkTextField(
        value = slug,
        onValueChange = onSlugChange,
        label = stringResource(R.string.slug),
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
