@file:Suppress("LongParameterList")

package com.clerk.ui.organizationprofile.root

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRowView
import com.clerk.ui.organizationprofile.custom.OrganizationProfileListRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileSection
import com.clerk.ui.organizationprofile.custom.buildOrganizationProfileRenderedRows
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.account.UserProfileIconActionRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun OrganizationProfileRootView(
  organization: Organization,
  membership: OrganizationMembership?,
  onBackPressed: () -> Unit,
  onUpdateProfile: () -> Unit,
  onAction: (OrganizationProfileAction) -> Unit,
  modifier: Modifier = Modifier,
  isDismissible: Boolean = true,
  customRows: ImmutableList<OrganizationProfileCustomRow> = persistentListOf(),
  onCustomRowClick: (String) -> Unit = {},
) {
  ClerkMaterialTheme {
    ClerkThemedProfileScaffold(
      modifier = modifier,
      title = stringResource(R.string.organization),
      backgroundColor = ClerkMaterialTheme.colors.muted,
      hasBackButton = isDismissible,
      horizontalPadding = dp0,
      onBackPressed = onBackPressed,
      content = {
        Spacers.Vertical.Spacer32()
        OrganizationProfileHeaderView(
          organization = organization,
          showsUpdateProfile = membership?.canManageProfile == true,
          onUpdateProfile = onUpdateProfile,
        )
        OrganizationProfileSectionRows(
          rows =
            buildOrganizationProfileRenderedRows(
              builtInRows = organizationProfileRows(membership = membership),
              section = OrganizationProfileSection.Profile,
              customRows = customRows,
            ),
          onAction = onAction,
          onCustomRowClick = onCustomRowClick,
        )
      },
      bottomContent = {
        OrganizationProfileSectionRows(
          rows =
            buildOrganizationProfileRenderedRows(
              builtInRows =
                organizationProfileActionRows(organization = organization, membership = membership),
              section = OrganizationProfileSection.Actions,
              customRows = customRows,
            ),
          onAction = onAction,
          onCustomRowClick = onCustomRowClick,
        )
      },
    )
  }
}

@Composable
private fun OrganizationProfileSectionRows(
  rows: List<OrganizationProfileListRow>,
  onAction: (OrganizationProfileAction) -> Unit,
  onCustomRowClick: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    rows.forEach { row ->
      when (row) {
        is OrganizationProfileListRow.BuiltIn ->
          OrganizationProfileBuiltInRow(row = row.row, onAction = onAction)
        is OrganizationProfileListRow.Custom ->
          OrganizationProfileCustomRowView(
            customRow = row.customRow,
            onClick = { onCustomRowClick(row.customRow.routeKey) },
          )
      }
      HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    }
  }
}

@Composable
private fun OrganizationProfileBuiltInRow(
  row: OrganizationProfileRow,
  onAction: (OrganizationProfileAction) -> Unit,
) {
  UserProfileIconActionRow(
    iconResId = row.iconResId,
    text = row.title(),
    onClick = { onAction(row.action) },
  )
}

private val OrganizationProfileRow.iconResId: Int
  get() =
    when (this) {
      OrganizationProfileRow.Members -> R.drawable.ic_users
      OrganizationProfileRow.VerifiedDomains -> R.drawable.ic_globe
      OrganizationProfileRow.LeaveOrganization -> R.drawable.ic_sign
      OrganizationProfileRow.DeleteOrganization -> R.drawable.ic_cross
    }

@Composable
private fun OrganizationProfileRow.title(): String {
  return when (this) {
    OrganizationProfileRow.Members -> stringResource(R.string.members)
    OrganizationProfileRow.VerifiedDomains -> stringResource(R.string.verified_domains)
    OrganizationProfileRow.LeaveOrganization -> stringResource(R.string.leave_organization)
    OrganizationProfileRow.DeleteOrganization -> stringResource(R.string.delete_organization)
  }
}

private val OrganizationProfileRow.action: OrganizationProfileAction
  get() =
    when (this) {
      OrganizationProfileRow.Members -> OrganizationProfileAction.Members
      OrganizationProfileRow.VerifiedDomains -> OrganizationProfileAction.VerifiedDomains
      OrganizationProfileRow.LeaveOrganization -> OrganizationProfileAction.LeaveOrganization
      OrganizationProfileRow.DeleteOrganization -> OrganizationProfileAction.DeleteOrganization
    }

internal enum class OrganizationProfileAction {
  Members,
  VerifiedDomains,
  UpdateProfile,
  LeaveOrganization,
  DeleteOrganization,
}
