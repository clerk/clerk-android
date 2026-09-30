package com.clerk.api.biometriccredential

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.configuration.ConfigurationManager
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.configuration.lifecycle.AppLifecycleListener
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.security.Key
import java.security.KeyPairGenerator
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.Signature
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.crypto.Cipher
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Pins the on-device biometric credential storage format that other Clerk SDKs in the same app
 * (e.g. `@clerk/expo-biometrics`) read and write (contract v2), and the legacy v1 format that is
 * migrated from. See `source/api/docs/biometric-credential-storage-contract.md`. The literals below
 * are the contract; update them only together with a contract version bump and a migration.
 */
@RunWith(RobolectricTestRunner::class)
class BiometricCredentialStorageContractTest {

  @get:Rule val temporaryFolder = TemporaryFolder()

  private lateinit var context: Context
  private lateinit var directory: File
  private val masterKey: SecretKey = SecretKeySpec(ByteArray(16) { it.toByte() }, "AES")
  private val keyStore = FakeAndroidKeyStore(mutableMapOf(MASTER_KEY_ALIAS to masterKey))

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    check(Security.getProvider(ANDROID_KEY_STORE) == null) {
      "Robolectric now provides AndroidKeyStore; drop the fake provider."
    }
    Security.insertProviderAt(FakeAndroidKeyStoreProvider(keyStore), 1)
    StorageHelper.storageCipherFactoryOverride = null
    StorageHelper.reset(context)
    preferences().edit(commit = true) { clear() }
    directory = File(temporaryFolder.root, DIRECTORY_NAME)
  }

  @After
  fun tearDown() {
    BiometricCredentialStorage.fileStore = null
    File(context.noBackupFilesDir, DIRECTORY_NAME).deleteRecursively()
    preferences().edit(commit = true) { clear() }
    StorageHelper.reset()
    Security.removeProvider(ANDROID_KEY_STORE)
  }

  @Test
  fun `v2 store identifiers match the contract`() {
    assertEquals(DIRECTORY_NAME, BiometricCredentialFileStore.DIRECTORY_NAME)
    assertEquals(DATA_FILE_NAME, BiometricCredentialFileStore.DATA_FILE_NAME)
    assertEquals(LOCK_FILE_NAME, BiometricCredentialFileStore.LOCK_FILE_NAME)
    assertEquals(2, BiometricCredentialFileStore.STORE_VERSION)
  }

  @Test
  fun `v2 store lives in the no backup files directory`() {
    BiometricCredentialStorage.initialize(context)
    val fileStore = BiometricCredentialStorage.requireFileStore()

    fileStore.saveCredential(fixtureRecords().first())

    val expectedDirectory = File(context.noBackupFilesDir, DIRECTORY_NAME)
    assertEquals(File(expectedDirectory, DATA_FILE_NAME), fileStore.dataFile)
    assertEquals(File(expectedDirectory, LOCK_FILE_NAME), fileStore.lockFile)
    assertTrue(fileStore.dataFile.isFile)
    assertTrue(fileStore.lockFile.isFile)
  }

  @Test
  fun `v2 store never touches clerk preferences or the Keystore`() {
    val fileStore =
      BiometricCredentialFileStore(directory, StorageHelperLegacyBiometricCredentialStore)

    fixtureRecords().forEach(fileStore::saveCredential)
    fileStore.addPendingCleanupUserId("user_1")
    fileStore.credentials()

    assertTrue(preferences().all.isEmpty())
    assertTrue(keyStore.requestedAliases.isEmpty())
    assertTrue(keyStore.generatedSpecs.isEmpty())
  }

  @Test
  fun `v2 file written by the SDK matches the schema`() {
    val fileStore = BiometricCredentialFileStore(directory)

    fixtureRecords().forEach(fileStore::saveCredential)
    fileStore.addPendingCleanupUserId("user_9")

    assertEquals(
      parse(
        """{
          "version":2,
          "credentials":[
            {"id":"td_current_set","local_key_id":"tdlk_0123456789abcdef0123456789abcdef",
              "user_id":"user_1","app_identifier":"com.example.app",
              "identifier_hint_sha256":"$USER_HINT_SHA256",
              "policy":"biometry_current_set","created_at":1735689600000,"updated_at":1735689600001},
            {"id":"td_any","local_key_id":"tdlk_fedcba9876543210fedcba9876543210",
              "user_id":"user_2","app_identifier":"com.example.app",
              "policy":"biometry_any","created_at":1735689700000,"updated_at":1735689700000},
            {"id":"td_device_passcode","local_key_id":"tdlk_00000000000000000000000000000000",
              "user_id":"user_3","app_identifier":"com.example.other",
              "policy":"biometry_or_device_passcode","created_at":1735689800000,
              "updated_at":1735689800000}
          ],
          "pending_cleanup_user_ids":["user_9"]
        }"""
      ),
      parse(dataFile().readText()),
    )
    assertFalse(File(directory, "$DATA_FILE_NAME.tmp").exists())
  }

  @Test
  fun `v2 file written by another SDK is readable`() {
    writeV2Fixture()

    assertEquals(fixtureRecords(), BiometricCredentialFileStore(directory).credentials())
    assertEquals(setOf("user_6"), BiometricCredentialFileStore(directory).pendingCleanupUserIds())
  }

  @Test
  fun `rewrites preserve unknown fields, unknown policies and undecodable records`() {
    writeV2Fixture()
    val fileStore = BiometricCredentialFileStore(directory)

    fileStore.addPendingCleanupUserId("user_0")
    fileStore.removePendingCleanupUserId("user_0")

    assertEquals(parse(fixture(V2, V2_FIXTURE)), parse(dataFile().readText()))

    val updated = fixtureRecords().first().copy(updatedAt = 1_735_689_600_002)
    fileStore.saveCredential(updated)
    fileStore.deleteCredential("td_any")

    val credentials = parse(dataFile().readText()).jsonObject.getValue("credentials").jsonArray
    assertEquals(
      listOf("td_current_set", "td_device_passcode", "td_future_policy", "td_incomplete"),
      credentials.map { it.jsonObject["id"]?.jsonPrimitive?.content },
    )
    val current = credentials.first().jsonObject
    assertEquals("1735689600002", current.getValue("updated_at").jsonPrimitive.content)
    assertEquals(parse("""{"nested":[1,2,3]}"""), current.getValue("future_field"))
    assertEquals(
      "some_future_policy",
      credentials[2].jsonObject.getValue("policy").jsonPrimitive.content,
    )
    assertEquals(
      parse("""{"written_by":"a newer SDK"}"""),
      parse(dataFile().readText()).jsonObject.getValue("future_top_level_key"),
    )
  }

  @Test
  fun `a different store version is read as empty and never modified`() {
    val future = """{"version":3,"credentials":[],"pending_cleanup_user_ids":[]}"""
    directory.mkdirs()
    dataFile().writeText(future)
    val fileStore = BiometricCredentialFileStore(directory)

    assertTrue(fileStore.credentials().isEmpty())
    assertFailsWith<IOException> { fileStore.saveCredential(fixtureRecords().first()) }
    assertEquals(future, dataFile().readText())
  }

  @Test
  fun `identifier hints are stored as the SHA-256 of the trimmed lowercase hint`() {
    assertEquals(
      USER_HINT_SHA256,
      BiometricCredentialLocalRecord.identifierHintSha256("  User@Example.COM\n"),
    )
    assertEquals(null, BiometricCredentialLocalRecord.identifierHintSha256("   "))
    assertTrue(fixtureRecords().first().matches("USER@example.com"))
    assertFalse(fixtureRecords().first().matches("other@example.com"))
  }

  @Test
  fun `concurrent writers in independent store instances do not lose updates`() {
    val stores = List(2) { BiometricCredentialFileStore(directory, processGuard = ReentrantLock()) }
    val writersPerStore = 4
    val recordsPerWriter = 15
    val executor = Executors.newFixedThreadPool(stores.size * writersPerStore)
    val start = CountDownLatch(1)
    try {
      val futures = stores.flatMapIndexed { storeIndex, fileStore ->
        List(writersPerStore) { writer ->
          executor.submit {
            start.await()
            repeat(recordsPerWriter) { index ->
              fileStore.saveCredential(
                fixtureRecords().first().copy(id = "td_${storeIndex}_${writer}_$index")
              )
            }
          }
        }
      }
      start.countDown()
      futures.forEach { it.get(30, TimeUnit.SECONDS) }
    } finally {
      executor.shutdownNow()
    }

    assertEquals(
      stores.size * writersPerStore * recordsPerWriter,
      BiometricCredentialFileStore(directory).credentials().map { it.id }.toSet().size,
    )
  }

  @Test
  fun `writers wait for the lock file to be released`() {
    val fileStore = BiometricCredentialFileStore(directory)
    directory.mkdirs()
    RandomAccessFile(File(directory, LOCK_FILE_NAME), "rw").channel.use { channel ->
      val held = channel.lock()
      val writer = thread { fileStore.saveCredential(fixtureRecords().first()) }

      writer.join(300)
      assertTrue(writer.isAlive)
      assertFalse(dataFile().exists())

      held.release()
      writer.join(5_000)
    }

    assertEquals(listOf(fixtureRecords().first()), fileStore.credentials())
  }

  @Test
  fun `writers give up when the lock is not released in time`() {
    val fileStore = BiometricCredentialFileStore(directory, lockTimeoutMillis = 100)
    directory.mkdirs()
    RandomAccessFile(File(directory, LOCK_FILE_NAME), "rw").channel.use { channel ->
      channel.lock().use {
        assertFailsWith<IOException> { fileStore.saveCredential(fixtureRecords().first()) }
      }
    }
    assertFalse(dataFile().exists())
  }

  @Test
  fun `v1 metadata is migrated into v2 and removed from clerk preferences`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device-token")
    writeRaw(CREDENTIALS_KEY, SpecCipher.encrypt(masterKey, fixture(V1, CREDENTIALS_FIXTURE)))
    writeRaw(PENDING_CLEANUP_KEY, SpecCipher.encrypt(masterKey, fixture(V1, CLEANUP_FIXTURE)))
    val existing = fixtureRecords()[1].copy(localKeyId = "tdlk_from_v2")
    BiometricCredentialFileStore(directory).run {
      saveCredential(existing)
      addPendingCleanupUserId("user_9")
    }

    val migrated =
      BiometricCredentialFileStore(directory, StorageHelperLegacyBiometricCredentialStore)
        .credentials()

    assertEquals(
      listOf(existing, fixtureRecords()[0], fixtureRecords()[2]),
      migrated,
    )
    assertEquals(
      parse("""["user_1","user_2","user_9"]"""),
      parse(dataFile().readText()).jsonObject.getValue("pending_cleanup_user_ids"),
    )
    assertFalse(dataFile().readText().contains("user@example.com"))
    assertTrue(dataFile().readText().contains("\"policy\":\"biometry_or_device_passcode\""))
    assertFalse(preferences().contains(CREDENTIALS_KEY))
    assertFalse(preferences().contains(PENDING_CLEANUP_KEY))
    assertEquals("device-token", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `configuring Clerk migrates v1 metadata without any biometric call`() = runTest {
    writeRaw(CREDENTIALS_KEY, SpecCipher.encrypt(masterKey, fixture(V1, CREDENTIALS_FIXTURE)))
    Clerk.reset()
    mockkObject(
      Client.Companion,
      Environment.Companion,
      NetworkConnectivityMonitor,
      AppLifecycleListener,
    )
    every { Client.serializer() } answers { callOriginal() }
    every { Environment.serializer() } answers { callOriginal() }
    coEvery { Client.get() } returns ClerkResult.unknownFailure(IOException("offline"))
    coEvery { Environment.get() } returns ClerkResult.unknownFailure(IOException("offline"))
    every { NetworkConnectivityMonitor.configure(any(), any()) } returns Unit
    every { AppLifecycleListener.configure(any()) } returns Unit
    val manager = ConfigurationManager(backgroundScope)
    try {
      manager.configure(
        context = context,
        publishableKey = "pk_test_biometric_migration",
        options = ClerkConfigurationOptions(proxyUrl = "https://proxy.example.com"),
      )
      runCurrent()

      val dataFile = File(File(context.noBackupFilesDir, DIRECTORY_NAME), DATA_FILE_NAME)
      assertTrue(dataFile.isFile)
      assertFalse(preferences().contains(CREDENTIALS_KEY))
      assertEquals(fixtureRecords(), BiometricCredentialStorage.requireFileStore().credentials())
    } finally {
      manager.reset()
      Clerk.reset()
      unmockkAll()
      NetworkConnectivityMonitor.resetForTesting()
    }
  }

  @Test
  fun `v1 migration is idempotent when interrupted before v1 is removed`() {
    val v1Credentials = SpecCipher.encrypt(masterKey, fixture(V1, CREDENTIALS_FIXTURE))
    writeRaw(CREDENTIALS_KEY, v1Credentials)
    BiometricCredentialFileStore(directory, StorageHelperLegacyBiometricCredentialStore)
      .credentials()
    val afterFirstRun = dataFile().readText()

    writeRaw(CREDENTIALS_KEY, v1Credentials)
    val fileStore =
      BiometricCredentialFileStore(directory, StorageHelperLegacyBiometricCredentialStore)

    assertEquals(fixtureRecords(), fileStore.credentials())
    assertEquals(parse(afterFirstRun), parse(dataFile().readText()))
    assertFalse(preferences().contains(CREDENTIALS_KEY))
  }

  @Test
  fun `v1 legacy identifiers are pinned for migration`() {
    assertEquals(PREFERENCES_FILE_NAME, CLERK_PREFERENCES_FILE_NAME)
    assertEquals(CREDENTIALS_KEY, StorageKey.TRUSTED_DEVICE_CREDENTIALS.name)
    assertEquals(PENDING_CLEANUP_KEY, StorageKey.PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP.name)
  }

  @Test
  fun `v1 records keep their legacy policy defaults when migrated`() {
    writeRaw(
      CREDENTIALS_KEY,
      SpecCipher.encrypt(
        masterKey,
        """[
          {"id":"td_null","local_key_id":"k2","user_id":"u","app_identifier":"a",
            "policy":null,"created_at":1,"updated_at":1},
          {"id":"td_unknown","local_key_id":"k3","user_id":"u","app_identifier":"a",
            "policy":"some_future_policy","created_at":1,"updated_at":1},
          {"id":"td_camel","localKeyId":"k4","userId":"u","appIdentifier":"a",
            "createdAt":1,"updatedAt":1}
        ]""",
      ),
    )

    val records =
      BiometricCredentialFileStore(directory, StorageHelperLegacyBiometricCredentialStore)
        .credentials()

    assertEquals(listOf("td_null", "td_unknown"), records.map { it.id })
    records.forEach {
      assertEquals(BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE, it.policy)
    }
  }

  @Test
  @Config(sdk = [30])
  fun `signing key parameters match the v1 contract on Android 11 and later`() {
    val expectations =
      mapOf(
        BiometricCredentialPolicy.BIOMETRY_CURRENT_SET to
          (KeyProperties.AUTH_BIOMETRIC_STRONG to true),
        BiometricCredentialPolicy.BIOMETRY_ANY to (KeyProperties.AUTH_BIOMETRIC_STRONG to false),
        BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE to
          ((KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL) to false),
      )

    expectations.forEach { (policy, expected) ->
      val (authenticators, invalidatedByEnrollment) = expected
      val spec = DefaultBiometricCredentialKeyManager.keyGenParameterSpec(LOCAL_KEY_ID, policy)

      assertEquals(KEY_ALIAS_PREFIX + LOCAL_KEY_ID, spec.keystoreAlias)
      assertEquals(KeyProperties.PURPOSE_SIGN, spec.purposes)
      assertArrayEquals(arrayOf(KeyProperties.DIGEST_SHA256), spec.digests)
      assertEquals("secp256r1", (spec.algorithmParameterSpec as ECGenParameterSpec).name)
      assertTrue(spec.isUserAuthenticationRequired)
      assertEquals(0, spec.userAuthenticationValidityDurationSeconds)
      assertEquals(authenticators, spec.userAuthenticationType)
      assertEquals(invalidatedByEnrollment, spec.isInvalidatedByBiometricEnrollment)
    }
  }

  @Test
  @Config(sdk = [28])
  fun `signing key parameters match the v1 contract before Android 11`() {
    BiometricCredentialPolicy.entries.forEach { policy ->
      val spec = DefaultBiometricCredentialKeyManager.keyGenParameterSpec(LOCAL_KEY_ID, policy)

      assertEquals(KEY_ALIAS_PREFIX + LOCAL_KEY_ID, spec.keystoreAlias)
      assertEquals(KeyProperties.PURPOSE_SIGN, spec.purposes)
      assertArrayEquals(arrayOf(KeyProperties.DIGEST_SHA256), spec.digests)
      assertEquals("secp256r1", (spec.algorithmParameterSpec as ECGenParameterSpec).name)
      assertTrue(spec.isUserAuthenticationRequired)
      assertEquals(-1, spec.userAuthenticationValidityDurationSeconds)
      assertEquals(
        policy == BiometricCredentialPolicy.BIOMETRY_CURRENT_SET,
        spec.isInvalidatedByBiometricEnrollment,
      )
    }
  }

  @Test
  fun `policy serial names match the v1 contract`() {
    assertEquals(
      mapOf(
        BiometricCredentialPolicy.BIOMETRY_CURRENT_SET to "\"biometry_current_set\"",
        BiometricCredentialPolicy.BIOMETRY_ANY to "\"biometry_any\"",
        BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE to "\"biometry_or_device_passcode\"",
      ),
      BiometricCredentialPolicy.entries.associateWith {
        Json.encodeToString(BiometricCredentialPolicy.serializer(), it)
      },
    )
  }

  @Test
  fun `raw signatures are IEEE P1363 r and s over the UTF-8 client data`() {
    val keyPair =
      KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
    val clientData = """{"challenge":"abc","type":"trusted_device.sign_in"}"""
    val der =
      Signature.getInstance("SHA256withECDSA").run {
        initSign(keyPair.private)
        update(clientData.toByteArray(Charsets.UTF_8))
        sign()
      }

    val raw = DefaultBiometricCredentialKeyManager.rawES256SignatureFromDer(der)

    assertEquals(64, raw.size)
    val verified =
      Signature.getInstance("SHA256withECDSAinP1363Format").run {
        initVerify(keyPair.public)
        update(clientData.toByteArray(Charsets.UTF_8))
        verify(raw)
      }
    assertTrue(verified)
  }

  private fun fixtureRecords(): List<BiometricCredentialLocalRecord> =
    listOf(
      BiometricCredentialLocalRecord(
        id = "td_current_set",
        localKeyId = "tdlk_0123456789abcdef0123456789abcdef",
        userId = "user_1",
        appIdentifier = "com.example.app",
        identifierHintSha256 = USER_HINT_SHA256,
        policy = BiometricCredentialPolicy.BIOMETRY_CURRENT_SET,
        createdAt = 1_735_689_600_000,
        updatedAt = 1_735_689_600_001,
      ),
      BiometricCredentialLocalRecord(
        id = "td_any",
        localKeyId = "tdlk_fedcba9876543210fedcba9876543210",
        userId = "user_2",
        appIdentifier = "com.example.app",
        policy = BiometricCredentialPolicy.BIOMETRY_ANY,
        createdAt = 1_735_689_700_000,
        updatedAt = 1_735_689_700_000,
      ),
      BiometricCredentialLocalRecord(
        id = "td_device_passcode",
        localKeyId = "tdlk_00000000000000000000000000000000",
        userId = "user_3",
        appIdentifier = "com.example.other",
        policy = BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE,
        createdAt = 1_735_689_800_000,
        updatedAt = 1_735_689_800_000,
      ),
    )

  private fun preferences(): SharedPreferences =
    context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

  private fun writeRaw(key: String, value: String) {
    preferences().edit(commit = true) { putString(key, value) }
  }

  private fun dataFile(): File = File(directory, DATA_FILE_NAME)

  private fun writeV2Fixture() {
    directory.mkdirs()
    dataFile().writeText(fixture(V2, V2_FIXTURE))
  }

  private fun fixture(version: String, name: String): String =
    checkNotNull(javaClass.getResourceAsStream("/biometric-credential-storage/$version/$name")) {
        "Missing fixture $version/$name"
      }
      .use { it.readBytes().toString(Charsets.UTF_8) }

  private fun parse(json: String): JsonElement = Json.parseToJsonElement(json)

  /** Minimal v1 value encryption written from the contract, not the SDK. */
  private object SpecCipher {
    fun encrypt(key: SecretKey, plaintext: String): String {
      val iv = ByteArray(GCM_IV_BYTES).also(SecureRandom()::nextBytes)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
      val sealed = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
      val encoder = Base64.getEncoder()
      return VALUE_PREFIX + encoder.encodeToString(iv) + ":" + encoder.encodeToString(sealed)
    }
  }

  private class FakeAndroidKeyStore(val keys: MutableMap<String, SecretKey>) {
    val requestedAliases = mutableListOf<String>()
    val generatedSpecs = mutableListOf<KeyGenParameterSpec>()
  }

  /**
   * Robolectric has no AndroidKeyStore, so this stands in for it with in-memory AES keys. The SDK's
   * production [com.clerk.api.storage.AndroidKeystoreStorageCipher] runs unmodified against it.
   */
  private class FakeAndroidKeyStoreProvider(state: FakeAndroidKeyStore) :
    Provider(ANDROID_KEY_STORE, 1.0, "Fake AndroidKeyStore for contract tests") {
    init {
      putService(
        FakeService(this, "KeyStore", ANDROID_KEY_STORE, KeyStoreSpi::class.java.name) {
          FakeKeyStoreSpi(state)
        }
      )
      putService(
        FakeService(this, "KeyGenerator", "AES", KeyGeneratorSpi::class.java.name) {
          FakeAesKeyGeneratorSpi(state)
        }
      )
    }
  }

  private class FakeService(
    provider: Provider,
    type: String,
    algorithm: String,
    className: String,
    private val factory: () -> Any,
  ) : Provider.Service(provider, type, algorithm, className, null, null) {
    override fun newInstance(constructorParameter: Any?): Any = factory()
  }

  private class FakeAesKeyGeneratorSpi(private val state: FakeAndroidKeyStore) : KeyGeneratorSpi() {
    private var spec: KeyGenParameterSpec? = null

    override fun engineInit(random: SecureRandom?) = throw UnsupportedOperationException()

    override fun engineInit(params: AlgorithmParameterSpec?, random: SecureRandom?) {
      spec = params as KeyGenParameterSpec
    }

    override fun engineInit(keysize: Int, random: SecureRandom?) =
      throw UnsupportedOperationException()

    override fun engineGenerateKey(): SecretKey {
      val spec = checkNotNull(spec)
      val key = SecretKeySpec(ByteArray(16).also(SecureRandom()::nextBytes), "AES")
      state.generatedSpecs += spec
      state.keys[spec.keystoreAlias] = key
      return key
    }
  }

  @Suppress("TooManyFunctions")
  private class FakeKeyStoreSpi(private val state: FakeAndroidKeyStore) : KeyStoreSpi() {
    override fun engineGetKey(alias: String, password: CharArray?): Key? {
      state.requestedAliases += alias
      return state.keys[alias]
    }

    override fun engineGetCertificateChain(alias: String?): Array<Certificate>? = null

    override fun engineGetCertificate(alias: String?): Certificate? = null

    override fun engineGetCreationDate(alias: String?): Date? = null

    override fun engineSetKeyEntry(
      alias: String?,
      key: Key?,
      password: CharArray?,
      chain: Array<out Certificate>?,
    ) = throw UnsupportedOperationException()

    override fun engineSetKeyEntry(
      alias: String?,
      key: ByteArray?,
      chain: Array<out Certificate>?,
    ) = throw UnsupportedOperationException()

    override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) =
      throw UnsupportedOperationException()

    override fun engineDeleteEntry(alias: String?) = throw UnsupportedOperationException()

    override fun engineAliases(): Enumeration<String> = Collections.enumeration(state.keys.keys)

    override fun engineContainsAlias(alias: String): Boolean = alias in state.keys

    override fun engineSize(): Int = state.keys.size

    override fun engineIsKeyEntry(alias: String): Boolean = alias in state.keys

    override fun engineIsCertificateEntry(alias: String?): Boolean = false

    override fun engineGetCertificateAlias(cert: Certificate?): String? = null

    override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit

    override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
  }

  private companion object {
    const val ANDROID_KEY_STORE = "AndroidKeyStore"
    const val PREFERENCES_FILE_NAME = "clerk_preferences"
    const val CREDENTIALS_KEY = "TRUSTED_DEVICE_CREDENTIALS"
    const val PENDING_CLEANUP_KEY = "PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP"
    const val VALUE_PREFIX = "clerk:v1:"
    const val MASTER_KEY_ALIAS = "clerk_preferences.master_key"
    const val KEY_ALIAS_PREFIX = "com.clerk.trusted_device."
    const val GCM_IV_BYTES = 12
    const val GCM_TAG_BITS = 128
    const val LOCAL_KEY_ID = "tdlk_0123456789abcdef0123456789abcdef"
    const val CREDENTIALS_FIXTURE = "trusted_device_credentials.json"
    const val CLEANUP_FIXTURE = "pending_trusted_device_credential_cleanup.json"
    const val V1 = "v1"
    const val V2 = "v2"
    const val V2_FIXTURE = "biometric_credentials.v2.json"
    const val DIRECTORY_NAME = "clerk"
    const val DATA_FILE_NAME = "biometric_credentials.v2.json"
    const val LOCK_FILE_NAME = "biometric_credentials.lock"
    const val USER_HINT_SHA256 = "b4c9a289323b21a01c3e940f150eb9b8c542587f1abfd8f0e1cc1ffc5e475514"
  }
}
