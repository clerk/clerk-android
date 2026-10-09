package com.clerk.ui.userprofile.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PendingBackupCodesViewModelTest {

  @Test
  fun pending_startsEmpty() {
    val viewModel = PendingBackupCodesViewModel()

    assertNull(viewModel.pending.value)
  }

  @Test
  fun show_holdsCodesAndMfaType() {
    val viewModel = PendingBackupCodesViewModel()

    viewModel.show(codes = listOf("code-1", "code-2"), mfaType = MfaType.AuthenticatorApp)

    assertEquals(
      PendingBackupCodes(codes = listOf("code-1", "code-2"), mfaType = MfaType.AuthenticatorApp),
      viewModel.pending.value,
    )
  }

  @Test
  fun show_defaultsToBackupCodesMfaType() {
    val viewModel = PendingBackupCodesViewModel()

    viewModel.show(codes = listOf("code-1"))

    assertEquals(MfaType.BackupCodes, viewModel.pending.value?.mfaType)
  }

  @Test
  fun clear_dropsHeldCodes() {
    val viewModel = PendingBackupCodesViewModel()
    viewModel.show(codes = listOf("code-1"))

    viewModel.clear()

    assertNull(viewModel.pending.value)
  }
}
