package com.clerk.ui.organizationprofile

import com.clerk.api.*
import com.clerk.ui.organizationprofile.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val OrganizationMembership.canManageProfile
  get() = "org:sys_profile:manage" in permissions
internal val OrganizationMembership.canDeleteOrganization
  get() = "org:sys_profile:delete" in permissions
internal val OrganizationMembership.canReadMemberships
  get() = "org:sys_memberships:read" in permissions
internal val OrganizationMembership.canManageMemberships
  get() = "org:sys_memberships:manage" in permissions
internal val OrganizationMembership.canReadDomains
  get() = "org:sys_domains:read" in permissions
internal val OrganizationMembership.canManageDomains
  get() = "org:sys_domains:manage" in permissions
internal val OrganizationDomain.isVerified
  get() = verification?.status == OrganizationDomainVerificationStatus.Verified

internal suspend fun File.organizationLogoInput(): SetOrganizationLogoParams =
  withContext(Dispatchers.IO) {
    SetOrganizationLogoParams(
      SetOrganizationLogoParamsFile.Case2(
        UploadFile(
          name,
          java.net.URLConnection.guessContentTypeFromName(name) ?: "application/octet-stream",
          readBytes(),
        )
      )
    )
  }
