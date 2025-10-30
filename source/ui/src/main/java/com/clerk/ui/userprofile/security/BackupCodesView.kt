package com.clerk.ui.userprofile.security

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.log.ClerkLog
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.PreviewUserProfileStateProvider
import com.clerk.ui.userprofile.UserProfileState
import java.io.File
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
internal fun BackupCodesView(
  codes: ImmutableList<String>,
  modifier: Modifier = Modifier,
  origin: Origin = Origin.BackupCodes,
  mfaType: MfaType = MfaType.BackupCodes,
) {

  val userProfileState = LocalUserProfileState.current
  BackHandler { handleBack(userProfileState, origin) }

  ClerkThemedProfileScaffold(
    title = stringResource(R.string.add_backup_code_verification),
    onBackPressed = { handleBack(userProfileState, origin) },
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp24)
          .then(modifier),
      verticalArrangement = Arrangement.spacedBy(dp24, alignment = Alignment.Top),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = mfaType.instructions(),
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )

      Surface(
        color = ClerkMaterialTheme.colors.background,
        modifier = Modifier.fillMaxWidth(),
        shape = ClerkMaterialTheme.shape,
        border = BorderStroke(width = dp1, color = ClerkMaterialTheme.computedColors.inputBorder),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = dp6),
            text = stringResource(R.string.backup_codes),
            color = ClerkMaterialTheme.colors.mutedForeground,
            style = ClerkMaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
          )
          BackupCodeGrid(codes = codes)
        }
      }
      ActionButtonRow(codes)
    }
  }
}

private const val POP_TO_PROFILE = 3

private fun handleBack(userProfileState: UserProfileState, origin: Origin) {
  if (origin == Origin.AuthenticatorApp) {
    userProfileState.pop(POP_TO_PROFILE)
  } else {
    userProfileState.navigateBack()
  }
}

@Composable
private fun ActionButtonRow(codes: ImmutableList<String>) {
  val context = LocalContext.current
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(dp24, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ClerkButton(
      modifier = Modifier.weight(1f),
      text = stringResource(R.string.download),
      onClick = { saveLinesToFileCompat(context, fileName = "backup_codes.txt", lines = codes) },
      configuration = ClerkButtonConfiguration(ClerkButtonConfiguration.ButtonStyle.Secondary),
    )
    ClerkButton(
      modifier = Modifier.weight(1f),
      text = stringResource(R.string.copy_to_clipboard),
      onClick = {
        scope.launch {
          val clipData = ClipData.newPlainText("Backup codes", codes.joinToString("\n"))
          clipboard.setClipEntry(ClipEntry(clipData))
        }
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
      },
      configuration = ClerkButtonConfiguration(ClerkButtonConfiguration.ButtonStyle.Secondary),
    )
  }
}

fun saveLinesToFileCompat(
  context: Context,
  fileName: String,
  lines: List<String>,
  mimeType: String = "text/plain",
) {
  try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      // Android 10+
      val resolver = context.contentResolver
      val values =
        ContentValues().apply {
          put(MediaStore.Downloads.DISPLAY_NAME, fileName)
          put(MediaStore.Downloads.MIME_TYPE, mimeType)
          put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
          put(MediaStore.Downloads.IS_PENDING, 1)
        }

      val fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
      resolver.openOutputStream(fileUri)?.use { output ->
        output.write(lines.joinToString("\n").toByteArray())
      }

      values.clear()
      values.put(MediaStore.Downloads.IS_PENDING, 0)
      resolver.update(fileUri, values, null, null)
      fileUri
    } else {
      // Pre-Android 10
      val downloadsDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      if (!downloadsDir.exists()) downloadsDir.mkdirs()
      val file = File(downloadsDir, fileName)
      file.writeText(lines.joinToString("\n"))
      MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
      Uri.fromFile(file)
    }

    Toast.makeText(
        context,
        context.getString(R.string.saved_to_downloads, fileName),
        Toast.LENGTH_SHORT,
      )
      .show()
  } catch (e: Exception) {
    ClerkLog.e("Failed to save file: ${e.message}")
    Toast.makeText(
        context,
        context.getString(R.string.failed_to_save_file, e.message),
        Toast.LENGTH_LONG,
      )
      .show()
  }
}

@Composable
fun BackupCodeGrid(codes: ImmutableList<String>, modifier: Modifier = Modifier) {
  LazyVerticalGrid(
    modifier = Modifier.fillMaxWidth().padding(horizontal = dp24).then(modifier),
    columns = GridCells.Fixed(count = 2),
  ) {
    items(codes) {
      Text(
        text = it,
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.foreground,
        modifier = Modifier.fillMaxWidth().padding(dp24),
        textAlign = TextAlign.Center,
      )
    }
  }
}

enum class Origin {
  AuthenticatorApp,
  BackupCodes,
}

@Composable
private fun MfaType.instructions() =
  when (this) {
    MfaType.PhoneCode ->
      stringResource(R.string.when_signing_in_you_will_need_to_enter_a_verification_code)
    MfaType.AuthenticatorApp -> stringResource(R.string.two_step_verification_is_now_enabled)
    MfaType.BackupCodes -> stringResource(R.string.backup_codes_are_now_enabled)
  }

internal enum class MfaType {
  PhoneCode,
  AuthenticatorApp,
  BackupCodes,
}

@PreviewLightDark
@Composable
private fun Preview() {
  PreviewUserProfileStateProvider {
    BackupCodesView(
      codes =
        persistentListOf(
          "jsdwz752",
          "abxkq983",
          "abxkq983",
          "mpltk294",
          "mpltk294",
          "qwert678",
          "dj2b5ugx",
          "xyztj501",
          "qwert678",
          "4nb52vql",
        ),
      mfaType = MfaType.AuthenticatorApp,
    )
  }
}
