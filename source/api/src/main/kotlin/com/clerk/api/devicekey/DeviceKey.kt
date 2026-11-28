package com.clerk.api.devicekey

import android.content.Context
import android.os.Build
import java.security.GeneralSecurityException
import java.security.PrivateKey
import java.security.ProviderException
import java.security.PublicKey
import java.security.Signature
import kotlinx.serialization.json.JsonObject

internal enum class DeviceKeySecurityLevel(val wireName: String) {
  STRONGBOX("strongbox"),
  TEE("tee"),
  SOFTWARE("software"),
}

internal interface DeviceKey {
  val thumbprint: String
  val publicJwk: JsonObject
  val securityLevel: DeviceKeySecurityLevel

  /**
   * Returns the ES256 signature of [data] as JWS encodes it: `r` then `s`, 32 bytes each (RFC 7518
   * section 3.4).
   */
  fun sign(data: ByteArray): ByteArray

  fun delete()
}

internal class DeviceKeyLostException(cause: Throwable? = null) :
  GeneralSecurityException("The device key is gone or permanently invalidated.", cause)

internal sealed interface DeviceKeyState {
  data class Ready(val key: DeviceKey) : DeviceKeyState

  data class Unavailable(val cause: Throwable) : DeviceKeyState
}

internal object DeviceKeyFactory {
  fun create(context: Context): DeviceKey =
    if (isRobolectric()) {
      JvmDeviceKey.getOrCreate()
    } else {
      AndroidKeystoreDeviceKey.getOrCreate(context)
    }

  fun delete(context: Context) {
    if (isRobolectric()) {
      JvmDeviceKey.deleteStored()
    } else {
      AndroidDeviceKeyStore(context).delete()
    }
  }

  private fun isRobolectric(): Boolean =
    Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
}

internal fun derSignature(privateKey: PrivateKey, data: ByteArray): ByteArray =
  Signature.getInstance(ES256_ALGORITHM).run {
    initSign(privateKey)
    update(data)
    sign()
  }

internal fun rawSignatureVerifiedBy(
  publicKey: PublicKey,
  data: ByteArray,
  derSignature: ByteArray,
): ByteArray {
  // On API 30 and lower a Keystore private key names its alias, not its key, so after the alias is
  // replaced it signs with the new key.
  val signedByPublicKey =
    Signature.getInstance(ES256_ALGORITHM).run {
      initVerify(publicKey)
      update(data)
      verify(derSignature)
    }
  if (!signedByPublicKey) {
    throw DeviceKeyLostException()
  }
  return Es256.rawSignatureFromDer(derSignature)
}

internal inline fun <T> catchingKeystoreFailures(block: () -> T): Result<T> =
  try {
    Result.success(block())
  } catch (e: GeneralSecurityException) {
    Result.failure(e)
  } catch (e: ProviderException) {
    Result.failure(e)
  }

private const val ES256_ALGORITHM = "SHA256withECDSA"
