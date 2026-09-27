package com.clerk.api.biometriccredential

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import java.io.InputStream
import java.io.OutputStream
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
import javax.crypto.Cipher
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Pins the on-device biometric credential storage format that other Clerk SDKs in the same app
 * (e.g. `@clerk/expo-biometrics`) read and write. See
 * `source/api/docs/biometric-credential-storage-contract.md`. The literals below are the contract;
 * update them only together with a contract version bump and a migration.
 */
@RunWith(RobolectricTestRunner::class)
class BiometricCredentialStorageContractTest {

  private lateinit var context: Context
  private val masterKey: SecretKey = SecretKeySpec(ByteArray(16) { it.toByte() }, "AES")
  private val keyStore = FakeAndroidKeyStore(mutableMapOf(MASTER_KEY_ALIAS to masterKey))
  private val store = DefaultBiometricCredentialLocalStore

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
  }

  @After
  fun tearDown() {
    preferences().edit(commit = true) { clear() }
    StorageHelper.reset()
    Security.removeProvider(ANDROID_KEY_STORE)
  }

  @Test
  fun `storage identifiers match the v1 contract`() {
    assertEquals(PREFERENCES_FILE_NAME, CLERK_PREFERENCES_FILE_NAME)
    assertEquals(CREDENTIALS_KEY, StorageKey.TRUSTED_DEVICE_CREDENTIALS.name)
    assertEquals(PENDING_CLEANUP_KEY, StorageKey.PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP.name)
  }

  @Test
  fun `storage master key is generated with the v1 parameters when absent`() {
    keyStore.keys.clear()
    StorageHelper.reset(context)

    store.save(fixtureRecords().first())

    val spec = keyStore.generatedSpecs.single()
    assertEquals(MASTER_KEY_ALIAS, spec.keystoreAlias)
    assertEquals(KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT, spec.purposes)
    assertArrayEquals(arrayOf(KeyProperties.BLOCK_MODE_GCM), spec.blockModes)
    assertArrayEquals(arrayOf(KeyProperties.ENCRYPTION_PADDING_NONE), spec.encryptionPaddings)
    assertTrue(spec.isRandomizedEncryptionRequired)
    assertFalse(spec.isUserAuthenticationRequired)
    assertEquals(-1, spec.keySize)
    val generatedKey = checkNotNull(keyStore.keys[MASTER_KEY_ALIAS])
    assertEquals(
      listOf(fixtureRecords().first()),
      store.all(),
    )
    assertTrue(
      SpecCipher.decrypt(generatedKey, checkNotNull(readRaw(CREDENTIALS_KEY)))
        .contains("\"td_current_set\"")
    )
  }

  @Test
  fun `credential metadata written by the SDK is readable by an independent implementation`() {
    fixtureRecords().forEach(store::save)

    val stored = checkNotNull(preferences().getString(CREDENTIALS_KEY, null))
    assertTrue(stored.startsWith(VALUE_PREFIX))
    val (iv, sealed) = stored.removePrefix(VALUE_PREFIX).split(':', limit = 2)
    val plaintext = SpecCipher.decrypt(masterKey, stored)

    assertEquals(GCM_IV_BYTES, Base64.getDecoder().decode(iv).size)
    assertEquals(
      plaintext.toByteArray(Charsets.UTF_8).size + GCM_TAG_BITS / Byte.SIZE_BITS,
      Base64.getDecoder().decode(sealed).size,
    )
    assertEquals(parse(fixture(CREDENTIALS_FIXTURE)), parse(plaintext))
    assertEquals(listOf(MASTER_KEY_ALIAS), keyStore.requestedAliases.distinct())
  }

  @Test
  fun `credential metadata written by an independent implementation is readable by the SDK`() {
    writeRaw(CREDENTIALS_KEY, SpecCipher.encrypt(masterKey, fixture(CREDENTIALS_FIXTURE)))

    assertEquals(fixtureRecords(), store.all())
  }

  @Test
  fun `records use snake case field names`() {
    writeRaw(
      CREDENTIALS_KEY,
      SpecCipher.encrypt(
        masterKey,
        """[{"id":"td_camel","localKeyId":"tdlk_1","userId":"user_1",
          "appIdentifier":"com.example.app","createdAt":1,"updatedAt":2}]""",
      ),
    )

    assertEquals(emptyList<BiometricCredentialLocalRecord>(), store.all())
  }

  @Test
  fun `policy defaults are decoded as biometry or device passcode`() {
    writeRaw(
      CREDENTIALS_KEY,
      SpecCipher.encrypt(
        masterKey,
        """[
          {"id":"td_explicit","local_key_id":"k1","user_id":"u","app_identifier":"a",
            "identifier_hint":null,"policy":"biometry_or_device_passcode",
            "created_at":1,"updated_at":1},
          {"id":"td_null","local_key_id":"k2","user_id":"u","app_identifier":"a",
            "policy":null,"created_at":1,"updated_at":1},
          {"id":"td_unknown","local_key_id":"k3","user_id":"u","app_identifier":"a",
            "policy":"some_future_policy","extra":"ignored","created_at":1,"updated_at":1}
        ]""",
      ),
    )

    val records = store.all()

    assertEquals(listOf("td_explicit", "td_null", "td_unknown"), records.map { it.id })
    records.forEach {
      assertEquals(BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE, it.policy)
      assertEquals(null, it.identifierHint)
    }
  }

  @Test
  fun `pending cleanup queue round trips with an independent implementation`() {
    writeRaw(PENDING_CLEANUP_KEY, SpecCipher.encrypt(masterKey, fixture(CLEANUP_FIXTURE)))

    assertEquals(setOf("user_1", "user_2"), BiometricCredentialPendingCleanupStore.all())

    BiometricCredentialPendingCleanupStore.add("user_0")

    assertEquals(
      parse("""["user_0","user_1","user_2"]"""),
      parse(SpecCipher.decrypt(masterKey, checkNotNull(readRaw(PENDING_CLEANUP_KEY)))),
    )

    listOf("user_0", "user_1", "user_2").forEach(BiometricCredentialPendingCleanupStore::remove)

    assertFalse(preferences().contains(PENDING_CLEANUP_KEY))
  }

  @Test
  fun `empty credential list removes the preference key`() {
    val record = fixtureRecords().first()
    store.save(record)
    store.delete(record.id)

    assertFalse(preferences().contains(CREDENTIALS_KEY))
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
        identifierHint = "user@example.com",
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

  private fun readRaw(key: String): String? = preferences().getString(key, null)

  private fun fixture(name: String): String =
    checkNotNull(javaClass.getResourceAsStream("/biometric-credential-storage/v1/$name")) {
        "Missing fixture $name"
      }
      .use { it.readBytes().toString(Charsets.UTF_8) }

  private fun parse(json: String): JsonElement = Json.parseToJsonElement(json)

  /** Minimal implementation of the value encryption written from the contract, not the SDK. */
  private object SpecCipher {
    fun encrypt(key: SecretKey, plaintext: String): String {
      val iv = ByteArray(GCM_IV_BYTES).also(SecureRandom()::nextBytes)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
      val sealed = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
      val encoder = Base64.getEncoder()
      return VALUE_PREFIX + encoder.encodeToString(iv) + ":" + encoder.encodeToString(sealed)
    }

    fun decrypt(key: SecretKey, stored: String): String {
      require(stored.startsWith(VALUE_PREFIX))
      val (iv, sealed) = stored.removePrefix(VALUE_PREFIX).split(':', limit = 2)
      val decoder = Base64.getDecoder()
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, decoder.decode(iv)))
      return cipher.doFinal(decoder.decode(sealed)).toString(Charsets.UTF_8)
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
  }
}
