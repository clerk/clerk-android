package com.clerk.ui.organizationprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.organizationprofile.root.OrganizationProfileRootView
import com.clerk.ui.theme.ClerkMaterialTheme

@PreviewLightDark
@Composable
private fun OrganizationProfileRootPreview() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background)) {
      OrganizationProfileRootView(
        organization = previewOrganizationProfileOrganization(),
        membership = previewOrganizationProfileMembership(),
        onBackPressed = {},
        onUpdateProfile = {},
        onAction = {},
      )
    }
  }
}
