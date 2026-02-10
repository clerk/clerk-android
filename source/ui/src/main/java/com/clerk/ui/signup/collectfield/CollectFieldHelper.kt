package com.clerk.ui.signup.collectfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
import com.clerk.api.Clerk
import com.clerk.ui.R

/**
 * A helper class for providing UI-related information for different [CollectField] types.
 *
 * This class contains methods to retrieve localized strings for titles, subtitles, and labels, as
 * well as a method to determine if a field is optional based on the Clerk configuration.
 */
@Stable
internal class CollectFieldHelper {
  @Composable
  fun title(collectField: CollectField) =
    when (collectField) {
      CollectField.Email -> stringResource(R.string.email_address)
      CollectField.Password -> stringResource(R.string.password)
      CollectField.Phone -> stringResource(R.string.phone_number)
      CollectField.Username -> stringResource(R.string.username)
    }

  @Composable
  fun subtitle(collectField: CollectField) =
    when (collectField) {
      CollectField.Email -> stringResource(R.string.enter_the_email_address_you_d_like_to_use)
      CollectField.Password -> stringResource(R.string.create_a_unique_password)
      CollectField.Phone -> stringResource(R.string.enter_the_phone_number_you_d_like_to_use)
      CollectField.Username -> stringResource(R.string.choose_a_username)
    }

  @Composable
  fun label(collectField: CollectField) =
    when (collectField) {
      CollectField.Email -> stringResource(R.string.enter_your_email)
      CollectField.Password -> stringResource(R.string.enter_your_password)
      CollectField.Phone -> stringResource(R.string.enter_your_phone_number)
      CollectField.Username -> stringResource(R.string.enter_your_username)
    }

  fun fieldIsOptional(collectField: CollectField) =
    Clerk.auth.currentSignUp?.requiredFields?.contains(collectField.rawValue) ?: true
}
