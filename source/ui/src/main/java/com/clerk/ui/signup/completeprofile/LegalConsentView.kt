package com.clerk.ui.signup.completeprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

/**
 * A composable that displays legal consent text with clickable links to Terms of Service and
 * Privacy Policy, along with a toggle switch for user acceptance.
 *
 * @param isAccepted Whether the user has accepted the legal terms.
 * @param onAcceptedChange Callback when the user toggles acceptance.
 * @param termsUrl The URL for the Terms of Service page, or null if not configured.
 * @param privacyPolicyUrl The URL for the Privacy Policy page, or null if not configured.
 * @param modifier Modifier for the composable.
 */
@Composable
internal fun LegalConsentView(
  isAccepted: Boolean,
  onAcceptedChange: (Boolean) -> Unit,
  termsUrl: String?,
  privacyPolicyUrl: String?,
  modifier: Modifier = Modifier,
) {
  val hasTerms = termsUrl != null
  val hasPrivacy = privacyPolicyUrl != null

  if (!hasTerms && !hasPrivacy) {
    return
  }

  val legalString =
    legalStringContent(
      hasTerms = hasTerms,
      termsUrl = termsUrl,
      hasPrivacy = hasPrivacy,
      privacyPolicyUrl = privacyPolicyUrl,
    )

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(ClerkMaterialTheme.design.borderRadius))
        .background(ClerkMaterialTheme.colors.muted)
        .padding(horizontal = 16.dp, vertical = dp8),
    horizontalArrangement = Arrangement.spacedBy(dp12),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = legalString,
      style = ClerkMaterialTheme.typography.bodyMedium,
      modifier = Modifier.weight(1f),
    )

    Switch(
      checked = isAccepted,
      onCheckedChange = onAcceptedChange,
      colors =
        SwitchDefaults.colors(
          checkedThumbColor = ClerkMaterialTheme.colors.primaryForeground,
          checkedTrackColor = ClerkMaterialTheme.colors.primary,
          uncheckedThumbColor = ClerkMaterialTheme.colors.mutedForeground,
          uncheckedTrackColor = ClerkMaterialTheme.colors.muted,
          uncheckedBorderColor = ClerkMaterialTheme.colors.border,
        ),
    )
  }
}

@Composable
private fun legalStringContent(
  hasTerms: Boolean,
  termsUrl: String?,
  hasPrivacy: Boolean,
  privacyPolicyUrl: String?,
): AnnotatedString {
  val iAgreeText = stringResource(R.string.i_agree_to_the)
  val termsText = stringResource(R.string.terms_of_service)
  val andText = stringResource(R.string.and_connector)
  val privacyText = stringResource(R.string.privacy_policy)

  val linkStyle = TextLinkStyles(style = SpanStyle(color = ClerkMaterialTheme.colors.primary))

  val annotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = ClerkMaterialTheme.colors.mutedForeground)) {
      append(iAgreeText)
      append(" ")
    }

    if (hasTerms && termsUrl != null) {
      withLink(LinkAnnotation.Url(url = termsUrl, styles = linkStyle)) { append(termsText) }

      if (hasPrivacy && privacyPolicyUrl != null) {
        withStyle(SpanStyle(color = ClerkMaterialTheme.colors.mutedForeground)) {
          append(" ")
          append(andText)
          append(" ")
        }
      }
    }

    if (hasPrivacy && privacyPolicyUrl != null) {
      withLink(LinkAnnotation.Url(url = privacyPolicyUrl, styles = linkStyle)) {
        append(privacyText)
      }
    }
  }
  return annotatedString
}

@PreviewLightDark
@Composable
private fun PreviewLegalConsentView_BothUrls() {
  ClerkMaterialTheme {
    LegalConsentView(
      isAccepted = false,
      onAcceptedChange = {},
      termsUrl = "https://example.com/terms",
      privacyPolicyUrl = "https://example.com/privacy",
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewLegalConsentView_OnlyTerms() {
  ClerkMaterialTheme {
    LegalConsentView(
      isAccepted = true,
      onAcceptedChange = {},
      termsUrl = "https://example.com/terms",
      privacyPolicyUrl = null,
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewLegalConsentView_OnlyPrivacy() {
  ClerkMaterialTheme {
    LegalConsentView(
      isAccepted = false,
      onAcceptedChange = {},
      termsUrl = null,
      privacyPolicyUrl = "https://example.com/privacy",
    )
  }
}
