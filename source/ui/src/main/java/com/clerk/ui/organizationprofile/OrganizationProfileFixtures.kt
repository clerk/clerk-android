package com.clerk.ui.organizationprofile

import androidx.compose.runtime.Composable
import com.clerk.api.*
import com.clerk.ui.core.preview.previewResource

@Composable
internal fun previewOrganizationProfileOrganization(): Organization =
  previewResource("Organization") as Organization

@Composable
internal fun previewOrganizationProfileMembership(): OrganizationMembership =
  previewResource("OrganizationMembership") as OrganizationMembership
