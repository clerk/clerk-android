package com.clerk.ui.userprofile.security.passkey.rename

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserProfilePasskeyRenameViewTest {

  @Test
  fun `updated passkey name is null when input is unchanged`() {
    assertNull(
      updatedPasskeyNameOrNull(
        passkeyNameInput = "Personal phone",
        originalPasskeyName = "Personal phone",
      )
    )
  }

  @Test
  fun `updated passkey name is null when input is blank`() {
    assertNull(
      updatedPasskeyNameOrNull(passkeyNameInput = " ", originalPasskeyName = "Personal phone")
    )
  }

  @Test
  fun `updated passkey name returns changed input`() {
    assertEquals(
      "Work laptop",
      updatedPasskeyNameOrNull(
        passkeyNameInput = "Work laptop",
        originalPasskeyName = "Personal phone",
      ),
    )
  }
}
