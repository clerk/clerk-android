package com.clerk.api.session

import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.UnknownFallbackEnumSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents the state of an in-session reverification (step-up) flow. */
@Serializable
public data class SessionVerification(
  /** The unique identifier for the verification attempt. */
  val id: String? = null,

  /** The current status of the verification. */
  val status: Status = Status.UNKNOWN,

  /** The required verification level. */
  val level: Level = Level.UNKNOWN,

  /** The session associated with the verification. */
  val session: Session? = null,

  /** First factors supported for this verification. */
  @SerialName("supported_first_factors") val supportedFirstFactors: List<Factor>? = null,

  /** Second factors supported for this verification. */
  @SerialName("supported_second_factors") val supportedSecondFactors: List<Factor>? = null,

  /** The state of the first-factor verification. */
  @SerialName("first_factor_verification") val firstFactorVerification: Verification? = null,

  /** The state of the second-factor verification. */
  @SerialName("second_factor_verification") val secondFactorVerification: Verification? = null,
) {
  /** The status of a session verification attempt. */
  @OptIn(ExperimentalSerializationApi::class)
  @KeepGeneratedSerializer
  @Serializable(with = Status.Serializer::class)
  public enum class Status {
    @SerialName("needs_first_factor") NEEDS_FIRST_FACTOR,
    @SerialName("needs_second_factor") NEEDS_SECOND_FACTOR,
    @SerialName("complete") COMPLETE,
    @SerialName("unknown") UNKNOWN;

    internal object Serializer :
      UnknownFallbackEnumSerializer<Status>(generatedSerializer(), UNKNOWN)
  }

  /** The required level of verification. */
  @OptIn(ExperimentalSerializationApi::class)
  @KeepGeneratedSerializer
  @Serializable(with = Level.Serializer::class)
  public enum class Level(public val value: String) {
    @SerialName("first_factor") FIRST_FACTOR("first_factor"),
    @SerialName("second_factor") SECOND_FACTOR("second_factor"),
    @SerialName("multi_factor") MULTI_FACTOR("multi_factor"),
    @SerialName("unknown") UNKNOWN("unknown");

    internal object Serializer :
      UnknownFallbackEnumSerializer<Level>(generatedSerializer(), UNKNOWN)
  }
}
