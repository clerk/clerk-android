package com.clerk.ui.sessiontask.organization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganizationSessionTaskUtilsTest {

  @Test
  fun `createOrganizationSlug lowercases and removes invalid separators`() {
    assertEquals("acme-inc", createOrganizationSlug(" Acme, Inc. "))
  }

  @Test
  fun `isValidOrganizationSlug accepts lowercase letters numbers and hyphens`() {
    assertTrue(isValidOrganizationSlug("acme-123"))
  }

  @Test
  fun `isValidOrganizationSlug rejects empty and uppercase values`() {
    assertFalse(isValidOrganizationSlug(""))
    assertFalse(isValidOrganizationSlug("Acme"))
  }

  @Test
  fun `failed initial load does not show no-organization help`() {
    val state =
      SessionTaskChooseOrganizationState(
        isLoading = false,
        initialLoadAttempted = true,
        hasLoadedInitialResources = false,
        canCreateOrganization = true,
      )

    assertTrue(state.initialLoadFailed)
    assertFalse(state.canShowNoOrganizationHelp)
  }

  @Test
  fun `successful empty initial load with creation enabled stays on chooser`() {
    val state =
      SessionTaskChooseOrganizationState(
        isLoading = false,
        initialLoadAttempted = true,
        hasLoadedInitialResources = true,
        canCreateOrganization = true,
      )

    assertFalse(state.initialLoadFailed)
    assertFalse(state.canShowNoOrganizationHelp)
  }

  @Test
  fun `successful empty initial load without creation can show no-organization help`() {
    val state =
      SessionTaskChooseOrganizationState(
        isLoading = false,
        initialLoadAttempted = true,
        hasLoadedInitialResources = true,
        canCreateOrganization = false,
      )

    assertTrue(state.canShowNoOrganizationHelp)
  }

  @Test
  fun `resource total counts keep chooser from showing no-organization help`() {
    val state =
      SessionTaskChooseOrganizationState(
        isLoading = false,
        initialLoadAttempted = true,
        hasLoadedInitialResources = true,
        membershipsTotalCount = 1,
      )

    assertTrue(state.hasExistingResources)
    assertFalse(state.canShowNoOrganizationHelp)
  }
}
