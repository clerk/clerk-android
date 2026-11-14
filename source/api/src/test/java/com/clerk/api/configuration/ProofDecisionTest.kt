package com.clerk.api.configuration

import com.clerk.api.configuration.DeviceBinding.NotSigning
import com.clerk.api.configuration.DeviceBinding.Reason.KEY_UNAVAILABLE
import com.clerk.api.configuration.DeviceBinding.Reason.SHARED_SESSION_SYNC
import com.clerk.api.configuration.DeviceBinding.Reason.TIMEOUT
import com.clerk.api.configuration.ProofDecision.FailRequest
import com.clerk.api.configuration.ProofDecision.Reason.KEY_ERROR
import com.clerk.api.configuration.ProofDecision.Reason.KEY_NOT_READY
import com.clerk.api.configuration.ProofDecision.SendUnsigned
import com.clerk.api.configuration.ProofDecision.Sign
import com.clerk.api.configuration.SignFailure.KEY_LOST
import com.clerk.api.configuration.SignFailure.NOT_A_LOSS
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.JvmDeviceKey
import kotlin.test.assertEquals
import org.junit.Test

class ProofDecisionTest {
  private val holder = DeviceKeyHolder { JvmDeviceKey.getOrCreate() }
  private val signing = DeviceBinding.Signing(holder)

  private data class Row(
    val tokenHasCnf: Boolean,
    val binding: DeviceBinding,
    val signFailure: SignFailure?,
    val expected: ProofDecision,
  )

  private val table =
    listOf(
      Row(tokenHasCnf = false, signing, signFailure = null, Sign(holder)),
      Row(tokenHasCnf = true, signing, signFailure = null, Sign(holder)),
      Row(tokenHasCnf = false, signing, KEY_LOST, SendUnsigned),
      Row(tokenHasCnf = true, signing, KEY_LOST, SendUnsigned),
      Row(tokenHasCnf = false, signing, NOT_A_LOSS, SendUnsigned),
      Row(tokenHasCnf = true, signing, NOT_A_LOSS, FailRequest(KEY_ERROR)),
      Row(tokenHasCnf = false, signing, SignFailure.TIMEOUT, SendUnsigned),
      Row(tokenHasCnf = true, signing, SignFailure.TIMEOUT, FailRequest(KEY_NOT_READY)),
      Row(tokenHasCnf = false, NotSigning(SHARED_SESSION_SYNC), signFailure = null, SendUnsigned),
      Row(tokenHasCnf = true, NotSigning(SHARED_SESSION_SYNC), signFailure = null, SendUnsigned),
      Row(tokenHasCnf = false, NotSigning(KEY_UNAVAILABLE), signFailure = null, SendUnsigned),
      Row(tokenHasCnf = true, NotSigning(KEY_UNAVAILABLE), signFailure = null, SendUnsigned),
      Row(tokenHasCnf = false, NotSigning(TIMEOUT), signFailure = null, SendUnsigned),
      Row(tokenHasCnf = true, NotSigning(TIMEOUT), signFailure = null, FailRequest(KEY_NOT_READY)),
    )

  @Test
  fun `each combination of cnf, binding, and sign failure gets the decision the policy names`() {
    val actual = table.map {
      it.copy(expected = proofDecision(it.tokenHasCnf, it.binding, it.signFailure))
    }

    assertEquals(table, actual)
  }

  @Test
  fun `the table covers every binding reason and every sign failure`() {
    assertEquals(
      DeviceBinding.Reason.entries.toSet(),
      table.mapNotNull { (it.binding as? NotSigning)?.reason }.toSet(),
    )
    assertEquals(
      SignFailure.entries.toSet(),
      table.filter { it.binding is DeviceBinding.Signing }.mapNotNull { it.signFailure }.toSet(),
    )
  }
}
