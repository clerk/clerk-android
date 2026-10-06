package com.clerk.ui.util

import android.content.Context
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.R

internal class TextIconHelper {
  fun actionText(factor: Factor, context: Context): String? {
    return when (factor.strategyType) {
      Strategy.PhoneCode -> {
        val safeIdentifier = factor.safeIdentifier
        if (safeIdentifier.isNullOrBlank()) {
          context.getString(R.string.send_sms_code)
        } else {
          context.getString(
            R.string.send_sms_code_to_phone,
            safeIdentifier.formattedAsPhoneNumberIfPossible,
          )
        }
      }
      Strategy.EmailCode -> {
        val safeIdentifier = factor.safeIdentifier
        if (safeIdentifier.isNullOrBlank()) {
          context.getString(R.string.email_code)
        } else {
          context.getString(R.string.email_code_to_email, safeIdentifier)
        }
      }
      Strategy.EmailLink -> {
        val safeIdentifier = factor.safeIdentifier
        if (safeIdentifier.isNullOrBlank()) {
          context.getString(R.string.send_email_to, context.getString(R.string.email_address))
        } else {
          context.getString(R.string.send_email_to, safeIdentifier)
        }
      }
      Strategy.Passkey -> context.getString(R.string.sign_in_with_your_passkey)
      Strategy.Password -> context.getString(R.string.sign_in_with_your_password)
      Strategy.Totp -> context.getString(R.string.use_your_authenticator_app)
      Strategy.BackupCode -> context.getString(R.string.use_a_backup_code)
      else -> null
    }
  }

  fun iconResource(factor: Factor): Int? {
    return when (factor.strategyType) {
      Strategy.PhoneCode -> R.drawable.ic_sms
      Strategy.EmailCode -> R.drawable.ic_email
      Strategy.EmailLink -> R.drawable.ic_email
      Strategy.Passkey -> R.drawable.ic_fingerprint
      Strategy.Password -> R.drawable.ic_lock
      else -> null
    }
  }
}
