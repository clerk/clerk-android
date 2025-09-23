package com.clerk.ui.signup.completeprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.clerk.ui.R
import kotlinx.collections.immutable.ImmutableList

/** Small helper to centralize enabled-fields, current focus, label, and validation logic. */
internal class CompleteProfileHelper(
  private val enabled: List<CompleteProfileField>,
  initial: CompleteProfileField,
) {
  var currentField by mutableStateOf(initial)
    private set

  fun focusTo(field: CompleteProfileField) {
    if (field in enabled) currentField = field
  }

  fun ensureValid() {
    if (currentField !in enabled && enabled.isNotEmpty()) {
      currentField = enabled.first()
    }
  }

  fun isSubmitEnabled(firstName: String, lastName: String): Boolean {
    val firstOk = if (CompleteProfileField.FirstName in enabled) firstName.isNotBlank() else true
    val lastOk = if (CompleteProfileField.LastName in enabled) lastName.isNotBlank() else true
    return firstOk && lastOk
  }

  fun submitLabelRes(): Int {
    val isLast = enabled.lastOrNull() == currentField
    return if (isLast) R.string.done else R.string.next
  }

  companion object {
    fun enabledFields(firstEnabled: Boolean, lastEnabled: Boolean): List<CompleteProfileField> =
      buildList {
        if (firstEnabled) add(CompleteProfileField.FirstName)
        if (lastEnabled) add(CompleteProfileField.LastName)
      }
  }
}

/** Remember an instance of [CompleteProfileHelper] for a given set of enabled fields. */
@Composable
internal fun rememberCompleteProfileHelper(
  enabledFields: ImmutableList<CompleteProfileField>
): CompleteProfileHelper {
  val initial = enabledFields.firstOrNull() ?: CompleteProfileField.FirstName
  val helper = remember(enabledFields) { CompleteProfileHelper(enabledFields, initial) }
  LaunchedEffect(enabledFields) { helper.ensureValid() }
  return helper
}
