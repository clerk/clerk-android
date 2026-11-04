package com.clerk.api.devicekey

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import kotlinx.serialization.json.JsonObject

internal class JvmDeviceKey private constructor(private val keyPair: KeyPair) : DeviceKey {
  override val publicJwk: JsonObject = Es256.publicJwk(keyPair.public as ECPublicKey)
  override val thumbprint: String = Es256.thumbprint(publicJwk)
  override val securityLevel: DeviceKeySecurityLevel = DeviceKeySecurityLevel.SOFTWARE

  override fun sign(data: ByteArray): ByteArray {
    val stored = synchronized(lock) { storedKeyPair } ?: throw DeviceKeyLostException()
    return rawSignatureVerifiedBy(keyPair.public, data, derSignature(stored.private, data))
  }

  override fun delete() {
    synchronized(lock) {
      if (storedKeyPair === keyPair) {
        storedKeyPair = null
      }
    }
  }

  companion object {
    private val lock = Any()
    private var storedKeyPair: KeyPair? = null

    fun getOrCreate(): JvmDeviceKey =
      synchronized(lock) {
        JvmDeviceKey(storedKeyPair ?: generateKeyPair().also { storedKeyPair = it })
      }

    private fun generateKeyPair(): KeyPair =
      KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
  }
}
