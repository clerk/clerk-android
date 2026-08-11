package com.clerk.ui.userprofile.connectedaccount

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.collections.immutable.toImmutableList

class UserProfileExternalAccountSectionTest : BaseSnapshotTest() {

  @Test
  fun highCardinalityAccountList_onlyComposesVisibleRows() {
    val externalAccounts =
      (0 until 20)
        .map { index ->
          ExternalAccount(
            id = "external_account_$index",
            identificationId = "identification_$index",
            provider = "oauth_google",
            providerUserId = "provider_user_$index",
            emailAddress = "user+$index@example.com",
            approvedScopes = "email profile",
            createdAt = index.toLong(),
          )
        }
        .toImmutableList()
    val composedAccountIds = mutableSetOf<String>()

    paparazzi.snapshot {
      ClerkMaterialTheme {
        LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp)) {
          userProfileExternalAccountSection(
            externalAccounts = externalAccounts,
            onError = {},
            onClickAddAccount = {},
            externalAccountRow = { externalAccount ->
              SideEffect { composedAccountIds += externalAccount.id }
              Box(modifier = Modifier.fillMaxWidth().height(64.dp))
            },
          )
        }
      }
    }

    assertTrue(composedAccountIds.isNotEmpty())
    assertTrue(
      composedAccountIds.size < externalAccounts.size,
      "Expected the lazy list to skip off-screen rows, but composed all ${externalAccounts.size}",
    )
  }
}
