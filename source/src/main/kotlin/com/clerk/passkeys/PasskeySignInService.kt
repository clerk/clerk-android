package com.clerk.passkeys

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.NoCredentialException
import com.clerk.Clerk
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import com.clerk.signin.SignIn
import com.clerk.signin.attemptFirstFactor
import com.clerk.sso.GoogleCredentialManagerImpl
import com.clerk.sso.GoogleSignInService
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.net.URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject

internal object PasskeySignInService {

  suspend fun signInWithPasskey(
    context: Context,
    allowedCredentialIds: List<String> = emptyList(),
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLog.e(
      "PasskeySignInService: Starting signInWithPasskey with allowedCredentialIds: $allowedCredentialIds"
    )

    return when (val createResult = createSignIn()) {
      is ClerkResult.Success -> {
        ClerkLog.e("PasskeySignInService: SignIn created successfully")
        val signIn = createResult.value
        ClerkLog.e("PasskeySignInService: SignIn object: $signIn")
        try {
          ClerkLog.e("PasskeySignInService: Getting credential from manager")
          val credential = getCredentialFromManager(context, signIn, allowedCredentialIds)
          ClerkLog.e("PasskeySignInService: Successfully obtained credential, handling it")
          handleCredential(credential, signIn)
        } catch (e: Exception) {
          ClerkLog.e("PasskeySignInService: Exception in signInWithPasskey: ${e.message}")
          ClerkResult.unknownFailure(e)
        }
      }
      is ClerkResult.Failure -> {
        ClerkLog.e("PasskeySignInService: Failed to create SignIn: ${createResult.error}")
        createResult
      }
    }
  }

  private suspend fun createSignIn(): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLog.e("PasskeySignInService: Creating SignIn with passkey strategy")
    val result = ClerkApi.signIn.createSignIn(mapOf("strategy" to "passkey"))
    ClerkLog.e("PasskeySignInService: CreateSignIn result: $result")
    return result
  }

  private suspend fun getCredentialFromManager(
    context: Context,
    signIn: SignIn,
    allowedCredentialIds: List<String> = emptyList(),
  ): Credential {
    ClerkLog.e(
      "PasskeySignInService: getCredentialFromManager called with allowedCredentialIds: $allowedCredentialIds"
    )

    val credentialManager = CredentialManager.create(context)
    ClerkLog.e("PasskeySignInService: CredentialManager created")

    val credentialRequest = buildCredentialRequest(signIn, allowedCredentialIds)
    ClerkLog.e("PasskeySignInService: CredentialRequest built: $credentialRequest")

    val result =
      try {
        ClerkLog.e("PasskeySignInService: Calling credentialManager.getCredential")
        credentialManager.getCredential(context, credentialRequest)
      } catch (e: NoCredentialException) {
        ClerkLog.e("PasskeySignInService: NoCredentialException: ${e.message}")
        throw e // Re-throw to be handled by the calling function
      } catch (e: Exception) {
        ClerkLog.e("PasskeySignInService: Error getting credential: ${e.message}")
        throw e // Re-throw to be handled by the calling function
      }

    ClerkLog.e(
      "PasskeySignInService: Successfully got credential result: ${result.credential::class.simpleName}"
    )
    return result.credential
  }

  private fun buildCredentialRequest(
    signIn: SignIn,
    allowedCredentialIds: List<String> = emptyList(),
  ): GetCredentialRequest {
    ClerkLog.e("PasskeySignInService: buildCredentialRequest called")
    ClerkLog.e(
      "PasskeySignInService: SignIn firstFactorVerification: ${signIn.firstFactorVerification}"
    )

    val requestJson =
      JSONObject(
        requireNotNull(signIn.firstFactorVerification?.nonce) {
          "Nonce is required for passkey authentication"
        }
      )
    val challenge = requestJson.get("challenge") as String
    ClerkLog.e("PasskeySignInService: Nonce extracted: $challenge")

    val getPasswordOption = GetPasswordOption()
    ClerkLog.e("PasskeySignInService: GetPasswordOption created")

    val getPublicKeyCredentialOption =
      buildPublicKeyCredentialOption(challenge, allowedCredentialIds)
    ClerkLog.e("PasskeySignInService: GetPublicKeyCredentialOption created")

    val googleIdOption = GoogleCredentialManagerImpl().getGoogleIdOption()
    ClerkLog.e("PasskeySignInService: GoogleIdOption created")

    val request =
      GetCredentialRequest(
        listOf(
          //        getPasswordOption,
          getPublicKeyCredentialOption
          //        googleIdOption,
        )
      )
    ClerkLog.e(
      "PasskeySignInService: GetCredentialRequest created with ${request.credentialOptions.size} options"
    )
    return request
  }

  private fun buildPublicKeyCredentialOption(
    challenge: String,
    allowedCredentialIds: List<String> = emptyList(),
  ): GetPublicKeyCredentialOption {
    ClerkLog.e(
      "PasskeySignInService: buildPublicKeyCredentialOption called with nonce:" +
        " $challenge, allowedCredentialIds: $allowedCredentialIds"
    )

    val allowCredentials =
      allowedCredentialIds.map { credentialId ->
        ClerkLog.e("PasskeySignInService: Adding allowed credential ID: $credentialId")
        mapOf("type" to "public-key", "id" to credentialId)
      }
    ClerkLog.e("PasskeySignInService: Built allowCredentials: $allowCredentials")

    val requestJson =
      WebAuthnRequest(
        challenge = challenge,
        allowCredentials = allowCredentials,
        timeout = 1800000,
        userVerification = "required",
        rpId = getDomain(),
      )
    ClerkLog.e("PasskeySignInService: WebAuthnRequest created: $requestJson")

    val jsonString = Json.encodeToString(requestJson)
    ClerkLog.e("PasskeySignInService: WebAuthnRequest JSON: $jsonString")

    return GetPublicKeyCredentialOption(requestJson = jsonString)
  }

  private suspend fun handleCredential(
    credential: Credential,
    signIn: SignIn,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLog.e(
      "PasskeySignInService: handleCredential called with credential type: ${credential::class.simpleName}"
    )
    ClerkLog.e("PasskeySignInService: Credential details: $credential")

    return when (credential) {
      is PublicKeyCredential -> {
        ClerkLog.e("PasskeySignInService: Handling PublicKeyCredential")
        handlePublicKeyCredential(credential, signIn)
      }

      is PasswordCredential -> {
        ClerkLog.e("PasskeySignInService: Handling PasswordCredential")
        ClerkLog.e("PasskeySignInService: PasswordCredential ID: ${credential.id}")
        ClerkLog.e(
          "PasskeySignInService: PasswordCredential password length: ${credential.password.length}"
        )

        ClerkResult.success(signIn)
      }

      is CustomCredential -> {
        ClerkLog.e("PasskeySignInService: Handling CustomCredential")
        handleCustomCredential(credential)
      }

      else -> {
        ClerkLog.e("PasskeySignInService: Unknown credential type: ${credential::class.simpleName}")
        ClerkResult.unknownFailure(
          error("Unknown credential type: ${credential::class.simpleName}")
        )
      }
    }
  }

  private suspend fun handlePublicKeyCredential(
    credential: PublicKeyCredential,
    signIn: SignIn,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLog.e("PasskeySignInService: handlePublicKeyCredential called")

    val responseJson = credential.authenticationResponseJson
    ClerkLog.e("PasskeySignInService: PublicKey authenticationResponseJson: $responseJson")

    ClerkLog.e("PasskeySignInService: Attempting first factor with passkey")
    val result = signIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.Passkey(responseJson))
    ClerkLog.e("PasskeySignInService: attemptFirstFactor result: $result")

    return result
  }

  private suspend fun handleCustomCredential(
    credential: CustomCredential
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    ClerkLog.e("PasskeySignInService: handleCustomCredential called with type: ${credential.type}")

    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      ClerkLog.e("PasskeySignInService: Processing GoogleIdTokenCredential")
      ClerkLog.e("PasskeySignInService: GoogleIdTokenCredential details: $credential")
    }

    ClerkLog.e("PasskeySignInService: Handling sign in result with GoogleSignInService")
    return when (val result = GoogleSignInService().handleSignInResult(credential)) {
      is ClerkResult.Success -> {
        ClerkLog.e("PasskeySignInService: GoogleSignInService success")
        val oauthResult = result.value
        ClerkLog.e("PasskeySignInService: OAuth result: $oauthResult")
        ClerkLog.e("PasskeySignInService: OAuth signIn: ${oauthResult.signIn}")
        ClerkResult.success(oauthResult.signIn!!)
      }
      is ClerkResult.Failure -> {
        ClerkLog.e("PasskeySignInService: GoogleSignInService failure: ${result.error}")
        result
      }
    }
  }

  private fun getDomain(): String {
    ClerkLog.e("PasskeySignInService: getDomain called with baseUrl: ${Clerk.baseUrl}")

    return try {
      val url = URL(Clerk.baseUrl)
      ClerkLog.e("PasskeySignInService: Parsed URL: $url")

      val host =
        url.host
          ?: return ""
            .also { ClerkLog.e("PasskeySignInService: URL host is null, returning empty string") }
      ClerkLog.e("PasskeySignInService: URL host: $host")

      val domain = host.replace("www.", "").replace("clerk.", "")
      ClerkLog.e("PasskeySignInService: Final domain: $domain")

      domain
    } catch (e: Exception) {
      ClerkLog.e("PasskeySignInService: Error parsing Clerk.baseUrl: ${e.message}")
      ""
    }
  }
}

@Serializable
data class WebAuthnRequest(
  val challenge: String,
  val allowCredentials: List<Map<String, String>> = emptyList(),
  val timeout: Long,
  val userVerification: String,
  val rpId: String,
)
