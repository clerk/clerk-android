package com.clerk.ui.userprofile.security

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class PendingBackupCodes(
  val codes: List<String>,
  val mfaType: MfaType = MfaType.BackupCodes,
)

internal class PendingBackupCodesViewModel : ViewModel() {

  private val _pending = MutableStateFlow<PendingBackupCodes?>(null)
  val pending: StateFlow<PendingBackupCodes?> = _pending.asStateFlow()

  fun show(codes: List<String>, mfaType: MfaType = MfaType.BackupCodes) {
    _pending.value = PendingBackupCodes(codes = codes, mfaType = mfaType)
  }

  fun clear() {
    _pending.value = null
  }
}
