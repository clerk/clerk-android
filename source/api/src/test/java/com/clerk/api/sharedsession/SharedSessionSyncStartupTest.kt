package com.clerk.api.sharedsession

import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import android.content.pm.Signature
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.SharedSessionSyncConfig
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.configuration.lifecycle.AppLifecycleListener
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.io.IOException
import java.util.Base64
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
class SharedSessionSyncStartupTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val sessionApi = mockk<SessionApi>()
  private val firstSessionTokenFetch = CountDownLatch(1)
  private val tokensSentBySessionTokenFetches =
    Collections.synchronizedList(mutableListOf<String?>())
  private val tokensSentByClientRequests = Collections.synchronizedList(mutableListOf<String?>())

  @Before
  fun setUp() {
    StorageHelper.initialize(context)
    StorageHelper.reset(context)
    Clerk.reset()
    mockkObject(Client.Companion, Environment.Companion, NetworkConnectivityMonitor, ClerkApi)
    mockkObject(AppLifecycleListener)
    every { Client.serializer() } answers { callOriginal() }
    every { Environment.serializer() } answers { callOriginal() }
    every { NetworkConnectivityMonitor.configure(any(), any()) } returns Unit
    every { AppLifecycleListener.configure(any()) } returns Unit
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.tokens(any(), any(), any(), any()) } coAnswers
      {
        tokensSentBySessionTokenFetches += StorageHelper.loadValue(StorageKey.DEVICE_TOKEN)
        firstSessionTokenFetch.countDown()
        ClerkResult.unknownFailure(IOException("offline"))
      }
    coEvery { Client.get() } coAnswers { offlineAfterRecordingToken() }
    coEvery { Client.getSkippingClientId() } coAnswers { offlineAfterRecordingToken() }
    coEvery { Environment.get() } coAnswers { offlineAfterRecordingToken() }
  }

  @After
  fun tearDown() {
    Clerk.reset()
    unmockkAll()
    StorageHelper.reset(context)
    NetworkConnectivityMonitor.resetForTesting()
  }

  @Test
  fun `a token a sibling synced before launch is the one the first session token fetch sends`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, LOCAL_DEVICE_TOKEN)
    signInLocally()
    siblingPublishes(SIBLING_DEVICE_TOKEN)

    initializeWithSharedSessionSync()

    coVerify(timeout = 5_000) { sessionApi.tokens(SESSION_ID, any(), any(), any()) }
    assertEquals(SIBLING_DEVICE_TOKEN, tokensSentBySessionTokenFetches.first())
  }

  @Test
  fun `a synced token that carries cnf is dropped before any request`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, LOCAL_DEVICE_TOKEN)
    signInLocally()
    siblingPublishes(BOUND_SIBLING_DEVICE_TOKEN)

    initializeWithSharedSessionSync()

    coVerify(timeout = 5_000) { Client.get() }
    coVerify(timeout = 5_000) { Environment.get() }
    val sent = tokensSentBySessionTokenFetches + tokensSentByClientRequests
    assertFalse("requests sent $sent", BOUND_SIBLING_DEVICE_TOKEN in sent)
    assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  private fun signInLocally() {
    val session =
      Session(
        id = SESSION_ID,
        status = Session.SessionStatus.ACTIVE,
        expireAt = Long.MAX_VALUE,
        lastActiveAt = 1_000,
        createdAt = 1_000,
        updatedAt = 1_000,
      )
    Clerk.updateClient(
      Client(id = "client_local", sessions = listOf(session), lastActiveSessionId = session.id),
      serverFetchAtMillis = 1_000,
    )
  }

  private fun siblingPublishes(deviceToken: String) {
    val snapshot =
      SharedSessionSyncSnapshot(
        instanceId = SharedSessionSyncSnapshot.instanceId(PUBLISHABLE_KEY),
        deviceToken =
          SharedSessionSyncSnapshot.DeviceTokenSnapshot(
            state = SharedSessionSyncSnapshot.State.SET,
            version = "sibling",
            changedAtMillis = 2_000,
            value = deviceToken,
          ),
      )
    val packageManager = shadowOf(context.packageManager)
    packageManager.getInternalMutablePackageInfo(context.packageName).signatures =
      arrayOf(CERTIFICATE)
    packageManager.installPackage(
      PackageInfo().apply {
        packageName = SIBLING_PACKAGE
        signatures = arrayOf(CERTIFICATE)
        applicationInfo = ApplicationInfo().apply { packageName = SIBLING_PACKAGE }
      }
    )
    val provider = ComponentName(SIBLING_PACKAGE, "SiblingSnapshotProvider")
    packageManager.addOrUpdateProvider(
      ProviderInfo().apply {
        packageName = provider.packageName
        name = provider.className
        authority = SIBLING_AUTHORITY
        exported = true
      }
    )
    packageManager.addIntentFilterForProvider(
      provider,
      IntentFilter(SharedSessionSyncContract.DISCOVERY_ACTION),
    )
    ShadowContentResolver.registerProviderInternal(
      SIBLING_AUTHORITY,
      SlowSiblingProvider(SharedSessionSyncSnapshot.encode(snapshot), firstSessionTokenFetch),
    )
  }

  private fun initializeWithSharedSessionSync() {
    Clerk.initialize(
      context = context,
      publishableKey = PUBLISHABLE_KEY,
      options =
        ClerkConfigurationOptions(
          proxyUrl = "https://proxy.example.com",
          sharedSessionSync = SharedSessionSyncConfig.enabled,
        ),
    )
  }

  private fun <T : Any> offlineAfterRecordingToken(): ClerkResult<T, Nothing> {
    tokensSentByClientRequests += StorageHelper.loadValue(StorageKey.DEVICE_TOKEN)
    return ClerkResult.unknownFailure(IOException("offline"))
  }

  private class SlowSiblingProvider(
    private val snapshot: String,
    private val sessionTokenFetchStarted: CountDownLatch,
  ) : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
      uri: Uri,
      projection: Array<out String>?,
      selection: String?,
      selectionArgs: Array<out String>?,
      sortOrder: String?,
    ): Cursor {
      sessionTokenFetchStarted.await(SIBLING_ANSWER_DELAY_MILLIS, TimeUnit.MILLISECONDS)
      return MatrixCursor(arrayOf(SharedSessionSyncContract.SNAPSHOT_COLUMN)).apply {
        addRow(arrayOf(snapshot))
      }
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
      uri: Uri,
      values: ContentValues?,
      selection: String?,
      selectionArgs: Array<out String>?,
    ): Int = 0
  }

  private companion object {
    const val PUBLISHABLE_KEY = "pk_test_shared_session_startup"
    const val SESSION_ID = "sess_local"
    const val SIBLING_PACKAGE = "com.example.sibling"
    const val SIBLING_AUTHORITY = SIBLING_PACKAGE + SharedSessionSyncContract.AUTHORITY_SUFFIX
    const val SIBLING_ANSWER_DELAY_MILLIS = 500L
    val CERTIFICATE = Signature("0a0b0c0d")
    val LOCAL_DEVICE_TOKEN = jwt("""{"id":"client_local"}""")
    val SIBLING_DEVICE_TOKEN = jwt("""{"id":"client_sibling"}""")
    val BOUND_SIBLING_DEVICE_TOKEN = jwt("""{"id":"client_sibling","cnf":{"jkt":"0ZcOCORZNYy"}}""")

    fun jwt(payload: String): String =
      listOf("""{"alg":"RS256"}""", payload).joinToString(".", postfix = ".signature") {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray())
      }
  }
}
