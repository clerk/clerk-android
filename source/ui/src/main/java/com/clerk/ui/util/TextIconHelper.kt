package com.clerk.ui.util

import android.content.Context
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.R
import com.clerk.ui.core.common.StrategyKeys

internal class TextIconHelper {
  /**
   * Returns the appropriate action text for a given sign-in factor.
   *
   * @param factor The [Factor] to get the action text for.
   * @param context The current Android [Context].
   * @return A formatted string describing the sign-in action.
   */
  fun actionText(factor: Factor, context: Context): String? {
    return when (factor.strategy) {
      StrategyKeys.PHONE_CODE -> {
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
      StrategyKeys.EMAIL_CODE -> {
        val safeIdentifier = factor.safeIdentifier
        if (safeIdentifier.isNullOrBlank()) {
          context.getString(R.string.email_code)
        } else {
          context.getString(R.string.email_code_to_email, safeIdentifier)
        }
      }
      StrategyKeys.PASSKEY -> context.getString(R.string.sign_in_with_your_passkey)
      StrategyKeys.PASSWORD -> context.getString(R.string.sign_in_with_your_password)
      StrategyKeys.TOTP -> context.getString(R.string.use_your_authenticator_app)
      StrategyKeys.BACKUP_CODE -> context.getString(R.string.use_a_backup_code)
      else -> null
    }
  }

  /**
   * Returns the appropriate icon resource for a given sign-in factor.
   *
   * @param factor The [Factor] to get the icon for.
   * @return A drawable resource ID for the factor's icon, or null if not applicable.
   */
  fun iconResource(factor: Factor): Int? {
    return when (factor.strategy) {
      StrategyKeys.PHONE_CODE -> R.drawable.ic_sms
      StrategyKeys.EMAIL_CODE -> R.drawable.ic_email
      StrategyKeys.PASSKEY -> R.drawable.ic_fingerprint
      StrategyKeys.PASSWORD -> R.drawable.ic_lock
      else -> null
    }
  }
}
