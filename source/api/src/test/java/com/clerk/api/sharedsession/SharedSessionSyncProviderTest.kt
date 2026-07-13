package com.clerk.api.sharedsession

import android.content.Context
import android.content.Intent
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SharedSessionSyncProviderTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.reset(context)
  }

  @After
  fun tearDown() {
    StorageHelper.reset(context)
  }

  @Test
  fun `merged manifest exposes discoverable read-only snapshot provider`() {
    val providers =
      context.packageManager.queryIntentContentProviders(
        Intent(SharedSessionSyncContract.DISCOVERY_ACTION),
        0,
      )
    val expectedAuthority = context.packageName + SharedSessionSyncContract.AUTHORITY_SUFFIX
    val provider = providers.firstOrNull { it.providerInfo.authority == expectedAuthority }

    assertNotNull(provider)
    assertTrue(provider!!.providerInfo.exported)

    StorageHelper.saveValue(StorageKey.SHARED_SESSION_SYNC_SNAPSHOT, "snapshot-json")
    val cursor =
      context.contentResolver.query(
        SharedSessionSyncContract.snapshotUri(expectedAuthority),
        arrayOf(SharedSessionSyncContract.SNAPSHOT_COLUMN),
        null,
        null,
        null,
      )

    cursor.use {
      assertTrue(it!!.moveToFirst())
      assertEquals(
        "snapshot-json",
        it.getString(it.getColumnIndexOrThrow(SharedSessionSyncContract.SNAPSHOT_COLUMN)),
      )
    }
  }
}
