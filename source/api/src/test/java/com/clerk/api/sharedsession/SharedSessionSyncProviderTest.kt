package com.clerk.api.sharedsession

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Process
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowApplicationPackageManager
import org.robolectric.shadows.ShadowBinder

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [UidSignatureComparingPackageManager::class])
class SharedSessionSyncProviderTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.reset(context)
  }

  @After
  fun tearDown() {
    SharedSessionSyncProvider.setEnabled(context, false)
    StorageHelper.reset(context)
  }

  @Test
  fun `provider is discoverable only while shared session sync is enabled`() {
    assertFalse(discoveredProviders().any { it.providerInfo.authority == expectedAuthority() })

    SharedSessionSyncProvider.setEnabled(context, true)

    val providers = discoveredProviders()
    val expectedAuthority = expectedAuthority()
    val provider = providers.firstOrNull { it.providerInfo.authority == expectedAuthority }

    assertNotNull(provider)
    assertTrue(provider!!.providerInfo.exported)
  }

  @Test
  fun `enabled provider exposes read-only snapshot`() {
    SharedSessionSyncProvider.setEnabled(context, true)
    val providers = discoveredProviders()
    val expectedAuthority = expectedAuthority()
    val provider = providers.firstOrNull { it.providerInfo.authority == expectedAuthority }

    assertNotNull(provider)
    assertTrue(provider!!.providerInfo.exported)

    StorageHelper.saveValue(StorageKey.SHARED_SESSION_SYNC_SNAPSHOT, "snapshot-json")

    assertEquals("snapshot-json", querySnapshot())
  }

  @Test
  fun `sibling app signed with a different certificate is refused the snapshot`() {
    SharedSessionSyncProvider.setEnabled(context, true)
    StorageHelper.saveValue(StorageKey.SHARED_SESSION_SYNC_SNAPSHOT, "snapshot-json")
    signOwnPackageWith(HOST_CERTIFICATE)
    val siblingUid = installSibling(signedWith = OTHER_CERTIFICATE)

    ShadowBinder.setCallingUid(siblingUid)

    assertThrows(SecurityException::class.java) { querySnapshot() }
  }

  @Test
  fun `sibling app signed with the same certificate reads the snapshot`() {
    SharedSessionSyncProvider.setEnabled(context, true)
    StorageHelper.saveValue(StorageKey.SHARED_SESSION_SYNC_SNAPSHOT, "snapshot-json")
    signOwnPackageWith(HOST_CERTIFICATE)
    val siblingUid = installSibling(signedWith = HOST_CERTIFICATE)

    ShadowBinder.setCallingUid(siblingUid)

    assertEquals("snapshot-json", querySnapshot())
  }

  private fun querySnapshot(): String? =
    context.contentResolver
      .query(
        SharedSessionSyncContract.snapshotUri(expectedAuthority()),
        arrayOf(SharedSessionSyncContract.SNAPSHOT_COLUMN),
        null,
        null,
        null,
      )
      .use {
        assertTrue(it!!.moveToFirst())
        it.getString(it.getColumnIndexOrThrow(SharedSessionSyncContract.SNAPSHOT_COLUMN))
      }

  private fun signOwnPackageWith(certificate: Signature) {
    val packageManager = shadowOf(context.packageManager)
    packageManager.getInternalMutablePackageInfo(context.packageName).signatures =
      arrayOf(certificate)
    packageManager.setPackagesForUid(Process.myUid(), context.packageName)
  }

  private fun installSibling(signedWith: Signature): Int {
    val siblingUid = Process.myUid() + 1
    val packageManager = shadowOf(context.packageManager)
    packageManager.installPackage(
      PackageInfo().apply {
        packageName = SIBLING_PACKAGE
        signatures = arrayOf(signedWith)
        applicationInfo =
          ApplicationInfo().apply {
            packageName = SIBLING_PACKAGE
            uid = siblingUid
          }
      }
    )
    packageManager.setPackagesForUid(siblingUid, SIBLING_PACKAGE)
    return siblingUid
  }

  private fun discoveredProviders() =
    context.packageManager.queryIntentContentProviders(
      Intent(SharedSessionSyncContract.DISCOVERY_ACTION),
      0,
    )

  private fun expectedAuthority(): String =
    context.packageName + SharedSessionSyncContract.AUTHORITY_SUFFIX

  private companion object {
    const val SIBLING_PACKAGE = "com.example.sibling"
    val HOST_CERTIFICATE = Signature("0a0b0c0d")
    val OTHER_CERTIFICATE = Signature("01020304")
  }
}

/**
 * Robolectric's `checkSignatures(int, int)` always returns `SIGNATURE_MATCH`, so this compares the
 * certificates of the packages installed under each uid, as the platform does.
 */
@Implements(className = "android.app.ApplicationPackageManager", isInAndroidSdk = false)
internal class UidSignatureComparingPackageManager : ShadowApplicationPackageManager() {
  @Implementation
  override fun checkSignatures(uid1: Int, uid2: Int): Int {
    val package1 = getPackagesForUid(uid1)?.firstOrNull()
    val package2 = getPackagesForUid(uid2)?.firstOrNull()
    return if (package1 == null || package2 == null) {
      PackageManager.SIGNATURE_UNKNOWN_PACKAGE
    } else {
      checkSignatures(package1, package2)
    }
  }
}
