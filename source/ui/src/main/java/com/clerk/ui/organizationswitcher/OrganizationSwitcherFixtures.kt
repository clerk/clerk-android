package com.clerk.ui.organizationswitcher

import androidx.compose.runtime.Composable
import com.clerk.api.*
import com.clerk.ui.core.preview.previewResource

@Composable
internal fun previewOrganizationMembership(): OrganizationMembership =
  previewResource("OrganizationMembership") as OrganizationMembership

@Composable internal fun previewOrganizationSwitcherUser(): User = previewResource("User") as User
