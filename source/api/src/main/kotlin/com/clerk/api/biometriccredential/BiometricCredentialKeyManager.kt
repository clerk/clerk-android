package com.clerk.api.biometriccredential

import android.content.Context
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.clerk.api.Clerk
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.ProviderException
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** A locally generated private key and its backend-facing public key material. */
internal data class BiometricCredentialLocalKey(
  val localKeyId: String,
  val publicKeyJwk: String,
  val algorithm: String = BiometricCredential.ES256_ALGORITHM,
  val policy: BiometricCredentialPolicy = BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE,
)

/** A signed biometric-credential challenge payload ready to send to Clerk. */
internal data class BiometricCredentialKeySignature(
  val clientData: String,
  val signature: String,
  val algorithm: String = BiometricCredential.ES256_ALGORITHM,
)

/**
 * An error produced by local biometric-credential key management or biometric authentication.
 *
 * Check [code] to distinguish user cancellation from real failures.
 */
class BiometricCredentialKeyManagerException
internal constructor(val code: Code, message: String, cause: Throwable? = null) :
  Exception(message, cause) {

  /** The category of biometric-credential key management error. */
  enum class Code {
    /** Biometric sign-in requires Android 9 (API 28) or later. */
    UNSUPPORTED_PLATFORM,

    /** Biometric authentication is not available or not enrolled on this device. */
    BIOMETRIC_AUTHENTICATION_UNAVAILABLE,

    /** The user canceled biometric authentication. */
    BIOMETRIC_AUTHENTICATION_CANCELED,

    /** Biometric authentication failed. */
    BIOMETRIC_AUTHENTICATION_FAILED,

    /** The biometric-credential private key could not be created. */
    KEY_GENERATION_FAILED,

    /** The biometric-credential private key was not found. */
    KEY_NOT_FOUND,

    /**
     * The biometric-credential private key was permanently invalidated (e.g. biometrics changed).
     */
    KEY_INVALIDATED,

    /** The biometric-credential challenge could not be signed. */
    SIGNING_FAILED,
  }
}

/** Manager for local biometric-credential private keys. */
internal interface BiometricCredentialKeyManager {
  /**
   * Whether biometric-credential keys protected by [policy] can be created and used on this device.
   */
  fun isSupported(policy: BiometricCredentialPolicy): Boolean

  /** Creates a new biometric-gated EC P-256 key pair and returns its public key material. */
  fun createKey(policy: BiometricCredentialPolicy): BiometricCredentialLocalKey

  /**
   * Signs [clientData] with the private key identified by [localKeyId], prompting the user for
   * biometric authentication.
   */
  suspend fun sign(
    clientData: String,
    localKeyId: String,
    policy: BiometricCredentialPolicy,
    promptTitle: String,
    promptSubtitle: String? = null,
  ): BiometricCredentialKeySignature

  /** Returns whether the private key identified by [localKeyId] exists. */
  fun hasKey(localKeyId: String): Boolean

  /** Deletes the private key identified by [localKeyId]. Missing keys are ignored. */
  fun deleteKey(localKeyId: String)
}

/**
 * Default [BiometricCredentialKeyManager] backed by the Android Keystore.
 *
 * Keys are EC P-256 signing keys requiring per-use user authentication. Challenge signing wraps a
 * [Signature] in a [BiometricPrompt.CryptoObject], so the signature can only be produced after the
 * user passes the system biometric prompt. Requires Android 9 (API 28) for [BiometricPrompt].
 */
@Suppress("TooManyFunctions")
internal object DefaultBiometricCredentialKeyManager : BiometricCredentialKeyManager {
  private const val ANDROID_KEY_STORE = "AndroidKeyStore"
  private const val KEY_ALIAS_PREFIX = "com.clerk.trusted_device."
  private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
  private const val EC_CURVE = "secp256r1"
  private const val COORDINATE_SIZE_BYTES = 32
  private const val LOCAL_KEY_ID_PREFIX = "tdlk_"
  private const val DER_SEQUENCE_TAG = 0x30
  private const val DER_INTEGER_TAG = 0x02
  private const val DER_LONG_FORM_FLAG = 0x80
  private const val DER_LENGTH_MASK = 0x7F
  private const val DER_BYTE_MASK = 0xFF

  override fun isSupported(policy: BiometricCredentialPolicy): Boolean {
    val context = applicationContext()
    val authenticators =
      if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
          policy == BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE
      ) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
          BiometricManager.Authenticators.DEVICE_CREDENTIAL
      } else {
        BiometricManager.Authenticators.BIOMETRIC_STRONG
      }
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
      context != null &&
      BiometricManager.from(context).canAuthenticate(authenticators) ==
        BiometricManager.BIOMETRIC_SUCCESS
  }

  override fun createKey(policy: BiometricCredentialPolicy): BiometricCredentialLocalKey {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      throw BiometricCredentialKeyManagerException(
        BiometricCredentialKeyManagerException.Code.UNSUPPORTED_PLATFORM,
        "Biometric sign-in requires Android 9 (API 28) or later.",
      )
    }
    if (!isSupported(policy)) {
      throw BiometricCredentialKeyManagerException(
        BiometricCredentialKeyManagerException.Code.BIOMETRIC_AUTHENTICATION_UNAVAILABLE,
        "Biometric authentication is not available or not enrolled on this device.",
      )
    }

    val localKeyId = makeLocalKeyId()
    try {
      val keyPairGenerator =
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEY_STORE)
      keyPairGenerator.initialize(keyGenParameterSpec(localKeyId, policy))
      val keyPair = keyPairGenerator.generateKeyPair()
      val publicKey = keyPair.public as ECPublicKey
      return BiometricCredentialLocalKey(
        localKeyId = localKeyId,
        publicKeyJwk = publicKeyJwk(publicKey),
        policy = policy,
      )
    } catch (e: BiometricCredentialKeyManagerException) {
      throw e
    } catch (e: GeneralSecurityException) {
      throw keyGenerationFailure(localKeyId, e)
    } catch (e: ProviderException) {
      throw keyGenerationFailure(localKeyId, e)
    } catch (e: IllegalStateException) {
      throw keyGenerationFailure(localKeyId, e)
    }
  }

  private fun keyGenerationFailure(
    localKeyId: String,
    cause: Exception,
  ): BiometricCredentialKeyManagerException {
    runCatching { deleteKey(localKeyId) }
    return BiometricCredentialKeyManagerException(
      BiometricCredentialKeyManagerException.Code.KEY_GENERATION_FAILED,
      "Unable to create the biometric-credential private key.",
      cause,
    )
  }

  override suspend fun sign(
    clientData: String,
    localKeyId: String,
    policy: BiometricCredentialPolicy,
    promptTitle: String,
    promptSubtitle: String?,
  ): BiometricCredentialKeySignature {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      throw BiometricCredentialKeyManagerException(
        BiometricCredentialKeyManagerException.Code.UNSUPPORTED_PLATFORM,
        "Biometric sign-in requires Android 9 (API 28) or later.",
      )
    }

    val signature = initializedSignature(localKeyId)
    val authenticatedSignature =
      authenticateWithBiometricPrompt(signature, policy, promptTitle, promptSubtitle)

    try {
      authenticatedSignature.update(clientData.toByteArray(Charsets.UTF_8))
      val derSignature = authenticatedSignature.sign()
      val rawSignature = rawES256SignatureFromDer(derSignature)
      return BiometricCredentialKeySignature(
        clientData = clientData,
        signature = base64UrlEncode(rawSignature),
      )
    } catch (e: BiometricCredentialKeyManagerException) {
      throw e
    } catch (e: Exception) {
      throw BiometricCredentialKeyManagerException(
        BiometricCredentialKeyManagerException.Code.SIGNING_FAILED,
        "Unable to sign the biometric-credential challenge.",
        e,
      )
    }
  }

  override fun hasKey(localKeyId: String): Boolean {
    return keyStore().containsAlias(keyAlias(localKeyId))
  }

  override fun deleteKey(localKeyId: String) {
    val keyStore = keyStore()
    val alias = keyAlias(localKeyId)
    if (keyStore.containsAlias(alias)) {
      keyStore.deleteEntry(alias)
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun initializedSignature(localKeyId: String): Signature {
    val privateKey =
      keyStore().getKey(keyAlias(localKeyId), null)
        ?: throw BiometricCredentialKeyManagerException(
          BiometricCredentialKeyManagerException.Code.KEY_NOT_FOUND,
          "The biometric-credential private key was not found.",
        )

    try {
      return Signature.getInstance(SIGNATURE_ALGORITHM).apply {
        initSign(privateKey as java.security.PrivateKey)
      }
    } catch (e: KeyPermanentlyInvalidatedException) {
      throw BiometricCredentialKeyManagerException(
        BiometricCredentialKeyManagerException.Code.KEY_INVALIDATED,
        "The biometric-credential private key was permanently invalidated.",
        e,
      )
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private suspend fun authenticateWithBiometricPrompt(
    signature: Signature,
    policy: BiometricCredentialPolicy,
    promptTitle: String,
    promptSubtitle: String?,
  ): Signature {
    val activity =
      Clerk.credentialActivity()
        ?: throw BiometricCredentialKeyManagerException(
          BiometricCredentialKeyManagerException.Code.SIGNING_FAILED,
          "Biometric sign-in requires an active Activity.",
        )

    return withContext(Dispatchers.Main) {
      suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }

        val executor = ContextCompat.getMainExecutor(activity)
        val builder =
          BiometricPrompt.Builder(activity).setTitle(promptTitle).apply {
            promptSubtitle?.let { setSubtitle(it) }
          }

        val allowsDeviceCredential =
          policy == BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        if (allowsDeviceCredential) {
          builder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
              BiometricManager.Authenticators.DEVICE_CREDENTIAL
          )
        } else {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
          }
          builder.setNegativeButton(
            activity.getString(android.R.string.cancel),
            executor,
            { _, _ ->
              if (continuation.isActive) {
                continuation.resumeWithException(
                  BiometricCredentialKeyManagerException(
                    BiometricCredentialKeyManagerException.Code.BIOMETRIC_AUTHENTICATION_CANCELED,
                    "Biometric authentication was canceled.",
                  )
                )
              }
            },
          )
        }

        val callback =
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
              if (!continuation.isActive) return
              val authenticatedSignature = result?.cryptoObject?.signature ?: signature
              continuation.resume(authenticatedSignature)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
              if (!continuation.isActive) return
              continuation.resumeWithException(authenticationError(errorCode, errString))
            }
          }

        builder
          .build()
          .authenticate(
            BiometricPrompt.CryptoObject(signature),
            cancellationSignal,
            executor,
            callback,
          )
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun authenticationError(
    errorCode: Int,
    errString: CharSequence?,
  ): BiometricCredentialKeyManagerException {
    val code =
      when (errorCode) {
        BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED,
        BiometricPrompt.BIOMETRIC_ERROR_CANCELED ->
          BiometricCredentialKeyManagerException.Code.BIOMETRIC_AUTHENTICATION_CANCELED
        BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS,
        BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT,
        BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
          BiometricCredentialKeyManagerException.Code.BIOMETRIC_AUTHENTICATION_UNAVAILABLE
        else -> BiometricCredentialKeyManagerException.Code.BIOMETRIC_AUTHENTICATION_FAILED
      }
    return BiometricCredentialKeyManagerException(
      code,
      errString?.toString() ?: "Biometric authentication failed.",
    )
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun keyGenParameterSpec(
    localKeyId: String,
    policy: BiometricCredentialPolicy,
  ): KeyGenParameterSpec {
    val builder =
      KeyGenParameterSpec.Builder(keyAlias(localKeyId), KeyProperties.PURPOSE_SIGN)
        .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
        .setDigests(KeyProperties.DIGEST_SHA256)
        .setUserAuthenticationRequired(true)
        .setInvalidatedByBiometricEnrollment(
          policy == BiometricCredentialPolicy.BIOMETRY_CURRENT_SET
        )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val authenticators =
        if (policy == BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE) {
          KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
        } else {
          KeyProperties.AUTH_BIOMETRIC_STRONG
        }
      builder.setUserAuthenticationParameters(0, authenticators)
    } else {
      @Suppress("DEPRECATION") builder.setUserAuthenticationValidityDurationSeconds(-1)
    }

    return builder.build()
  }

  private fun publicKeyJwk(publicKey: ECPublicKey): String {
    val x = base64UrlEncode(fixedWidthCoordinate(publicKey.w.affineX))
    val y = base64UrlEncode(fixedWidthCoordinate(publicKey.w.affineY))
    return """{"kty":"EC","crv":"P-256","x":"$x","y":"$y","alg":"ES256"}"""
  }

  private fun fixedWidthCoordinate(coordinate: BigInteger): ByteArray {
    val bytes = coordinate.toByteArray()
    return when {
      bytes.size == COORDINATE_SIZE_BYTES -> bytes
      // Strip the sign byte added by BigInteger for values with the high bit set.
      bytes.size > COORDINATE_SIZE_BYTES ->
        bytes.copyOfRange(bytes.size - COORDINATE_SIZE_BYTES, bytes.size)
      else -> ByteArray(COORDINATE_SIZE_BYTES - bytes.size) + bytes
    }
  }

  internal fun rawES256SignatureFromDer(signature: ByteArray): ByteArray {
    val reader = DerReader(signature)
    validateES256Signature(reader.readByte() == DER_SEQUENCE_TAG)

    val sequenceLength = reader.readLength()
    validateES256Signature(sequenceLength == reader.remainingBytes)

    val r = reader.readInteger()
    val s = reader.readInteger()
    validateES256Signature(reader.remainingBytes == 0)

    return paddedES256Component(r) + paddedES256Component(s)
  }

  private fun validateES256Signature(isValid: Boolean) {
    if (!isValid) {
      throw invalidES256Signature()
    }
  }

  private fun paddedES256Component(component: ByteArray): ByteArray {
    if (component.isEmpty() || component.first().toInt() and DER_LONG_FORM_FLAG != 0) {
      throw invalidES256Signature()
    }

    var startIndex = 0
    while (
      component.size - startIndex > COORDINATE_SIZE_BYTES && component[startIndex].toInt() == 0
    ) {
      startIndex += 1
    }

    val componentSize = component.size - startIndex
    if (componentSize !in 1..COORDINATE_SIZE_BYTES) {
      throw invalidES256Signature()
    }

    return ByteArray(COORDINATE_SIZE_BYTES).also { padded ->
      component.copyInto(
        destination = padded,
        destinationOffset = COORDINATE_SIZE_BYTES - componentSize,
        startIndex = startIndex,
      )
    }
  }

  private fun invalidES256Signature(): BiometricCredentialKeyManagerException {
    return BiometricCredentialKeyManagerException(
      BiometricCredentialKeyManagerException.Code.SIGNING_FAILED,
      "Android Keystore returned an invalid ES256 signature.",
    )
  }

  private class DerReader(private val bytes: ByteArray) {
    private var offset: Int = 0

    val remainingBytes: Int
      get() = bytes.size - offset

    fun readByte(): Int {
      if (offset >= bytes.size) {
        throw invalidES256Signature()
      }
      return bytes[offset++].toInt() and DER_BYTE_MASK
    }

    fun readLength(): Int {
      val first = readByte()
      if (first and DER_LONG_FORM_FLAG == 0) {
        return first
      }

      val byteCount = first and DER_LENGTH_MASK
      if (byteCount == 0 || byteCount > Int.SIZE_BYTES || byteCount > remainingBytes) {
        throw invalidES256Signature()
      }

      var length = 0
      repeat(byteCount) { length = (length shl Byte.SIZE_BITS) or readByte() }
      return length
    }

    fun readInteger(): ByteArray {
      if (readByte() != DER_INTEGER_TAG) {
        throw invalidES256Signature()
      }

      val length = readLength()
      if (length <= 0 || length > remainingBytes) {
        throw invalidES256Signature()
      }

      return bytes.copyOfRange(offset, offset + length).also { offset += length }
    }
  }

  private fun base64UrlEncode(bytes: ByteArray): String {
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
  }

  private fun makeLocalKeyId(): String {
    return LOCAL_KEY_ID_PREFIX + java.util.UUID.randomUUID().toString().replace("-", "").lowercase()
  }

  private fun keyAlias(localKeyId: String): String = KEY_ALIAS_PREFIX + localKeyId

  private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

  private fun applicationContext(): Context? = Clerk.applicationContext?.get()
}
