package com.clerk.api.network.serialization

import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.user.User
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class FormFieldsTest {

  @Test
  fun `sign-in create strategies send their fields and strategy`() {
    val fields =
      listOf<SignIn.CreateParams.Strategy>(
          SignIn.CreateParams.Strategy.EmailCode(identifier = "a+clerk_test@example.com"),
          SignIn.CreateParams.Strategy.PhoneCode(identifier = "+15555550100"),
          SignIn.CreateParams.Strategy.Password(identifier = "alice", password = "pw"),
          SignIn.CreateParams.Strategy.ResetPasswordEmailCode(identifier = "alice"),
          SignIn.CreateParams.Strategy.ResetPasswordPhoneCode(identifier = "alice"),
          SignIn.CreateParams.Strategy.Ticket(ticket = "tkt"),
          SignIn.CreateParams.Strategy.Identifier(identifier = "alice"),
          SignIn.CreateParams.Strategy.Identifier(
            strategy = "password",
            identifier = "alice",
            password = "pw",
          ),
        )
        .map { it.toFormFields() }

    assertEquals(
      listOf(
        mapOf("identifier" to "a+clerk_test@example.com", "strategy" to "email_code"),
        mapOf("identifier" to "+15555550100", "strategy" to "phone_code"),
        mapOf("identifier" to "alice", "password" to "pw", "strategy" to "password"),
        mapOf("identifier" to "alice", "strategy" to "reset_password_email_code"),
        mapOf("identifier" to "alice", "strategy" to "reset_password_phone_code"),
        mapOf("ticket" to "tkt", "strategy" to "ticket"),
        mapOf("identifier" to "alice"),
        mapOf("strategy" to "password", "identifier" to "alice", "password" to "pw"),
      ),
      fields,
    )
  }

  @Test
  fun `sign-in first factor attempts send code, password or credential`() {
    val fields =
      listOf<SignIn.AttemptFirstFactorParams>(
          SignIn.AttemptFirstFactorParams.EmailCode(code = "424242"),
          SignIn.AttemptFirstFactorParams.PhoneCode(code = "424242"),
          SignIn.AttemptFirstFactorParams.Password(password = "pw"),
          SignIn.AttemptFirstFactorParams.Passkey(publicKeyCredential = "cred"),
          SignIn.AttemptFirstFactorParams.ResetPasswordEmailCode(code = "424242"),
          SignIn.AttemptFirstFactorParams.ResetPasswordPhoneCode(code = "424242"),
        )
        .map { it.toFormFields() }

    assertEquals(
      listOf(
        mapOf("code" to "424242", "strategy" to "email_code"),
        mapOf("code" to "424242", "strategy" to "phone_code"),
        mapOf("password" to "pw", "strategy" to "password"),
        mapOf("public_key_credential" to "cred", "strategy" to "passkey"),
        mapOf("code" to "424242", "strategy" to "reset_password_email_code"),
        mapOf("code" to "424242", "strategy" to "reset_password_phone_code"),
      ),
      fields,
    )
  }

  @Test
  fun `sign-in second factor attempts send code or credential`() {
    val fields =
      listOf<SignIn.AttemptSecondFactorParams>(
          SignIn.AttemptSecondFactorParams.PhoneCode(code = "424242"),
          SignIn.AttemptSecondFactorParams.TOTP(code = "424242"),
          SignIn.AttemptSecondFactorParams.BackupCode(code = "backup"),
          SignIn.AttemptSecondFactorParams.EmailCode(code = "424242"),
          SignIn.AttemptSecondFactorParams.Passkey(publicKeyCredential = "cred"),
        )
        .map { it.toFormFields() }

    assertEquals(
      listOf(
        mapOf("code" to "424242", "strategy" to "phone_code"),
        mapOf("code" to "424242", "strategy" to "totp"),
        mapOf("code" to "backup", "strategy" to "backup_code"),
        mapOf("code" to "424242", "strategy" to "email_code"),
        mapOf("public_key_credential" to "cred", "strategy" to "passkey"),
      ),
      fields,
    )
  }

  @Test
  fun `sign-in first factor preparation sends the factor id and strategy`() {
    val fields =
      listOf<SignIn.PrepareFirstFactorParams>(
          SignIn.PrepareFirstFactorParams.EmailCode(emailAddressId = "idn_email"),
          SignIn.PrepareFirstFactorParams.PhoneCode(phoneNumberId = "idn_phone"),
          SignIn.PrepareFirstFactorParams.EmailLink(
            emailAddressId = "idn_email",
            redirectUri = "clerk://callback",
            codeChallenge = "challenge",
          ),
          SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(emailAddressId = "idn_email"),
          SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode(phoneNumberId = "idn_phone"),
          SignIn.PrepareFirstFactorParams.OAuth(
            strategy = "oauth_google",
            redirectUrl = "clerk://callback",
          ),
          SignIn.PrepareFirstFactorParams.EnterpriseSSO(redirectUrl = "clerk://callback"),
          SignIn.PrepareFirstFactorParams.Passkey(),
        )
        .map { it.toFormFields() }

    assertEquals(
      listOf(
        mapOf("email_address_id" to "idn_email", "strategy" to "email_code"),
        mapOf("phone_number_id" to "idn_phone", "strategy" to "phone_code"),
        mapOf(
          "email_address_id" to "idn_email",
          "redirect_uri" to "clerk://callback",
          "code_challenge" to "challenge",
          "code_challenge_method" to "S256",
          "strategy" to "email_link",
        ),
        mapOf("email_address_id" to "idn_email", "strategy" to "reset_password_email_code"),
        mapOf("phone_number_id" to "idn_phone", "strategy" to "reset_password_phone_code"),
        mapOf("strategy" to "oauth_google", "redirect_url" to "clerk://callback"),
        mapOf("strategy" to "enterprise_sso", "redirect_url" to "clerk://callback"),
        mapOf("strategy" to "passkey"),
      ),
      fields,
    )
  }

  @Test
  fun `sign-in second factor preparation omits ids that are not set`() {
    assertEquals(
      mapOf("strategy" to "phone_code"),
      SignIn.PrepareSecondFactorParams().toFormFields(),
    )
    assertEquals(
      mapOf(
        "strategy" to "email_code",
        "phone_number_id" to "idn_phone",
        "email_address_id" to "idn_email",
      ),
      SignIn.PrepareSecondFactorParams(
          strategy = "email_code",
          phoneNumberId = "idn_phone",
          emailAddressId = "idn_email",
        )
        .toFormFields(),
    )
  }

  @Test
  fun `sign-up create sends only the fields that are set`() {
    assertEquals(emptyMap<String, String>(), sendSignUpCreate(SignUp.CreateParams.Standard()))
    assertEquals(
      mapOf(
        "email_address" to "a+clerk_test@example.com",
        "password" to "pw",
        "first_name" to "Ada",
        "last_name" to "Lovelace",
        "username" to "ada",
        "phone_number" to "+15555550100",
        "legal_accepted" to "true",
        "unsafe_metadata" to NESTED_METADATA_JSON,
      ),
      sendSignUpCreate(
        SignUp.CreateParams.Standard(
          emailAddress = "a+clerk_test@example.com",
          password = "pw",
          firstName = "Ada",
          lastName = "Lovelace",
          username = "ada",
          phoneNumber = "+15555550100",
          legalAccepted = true,
          unsafeMetadata = NESTED_METADATA,
        )
      ),
    )
  }

  @Test
  fun `sign-up update sends only the fields that are set`() {
    val update: SignUp.SignUpUpdateParams =
      SignUp.SignUpUpdateParams.Standard(
        emailAddress = "a+clerk_test@example.com",
        password = "pw",
        firstName = "Ada",
        lastName = "Lovelace",
        username = "ada",
        phoneNumber = "+15555550100",
        legalAccepted = false,
      )

    assertEquals(
      mapOf(
        "email_address" to "a+clerk_test@example.com",
        "password" to "pw",
        "first_name" to "Ada",
        "last_name" to "Lovelace",
        "username" to "ada",
        "phone_number" to "+15555550100",
        "legal_accepted" to "false",
      ),
      update.toFormFields(),
    )
  }

  @Test
  fun `user update sends metadata strings as they are`() {
    assertEquals(emptyMap<String, String>(), User.UpdateParams().toFormFields())
    assertEquals(
      mapOf(
        "first_name" to "Ada",
        "last_name" to "Lovelace",
        "username" to "ada",
        "primary_email_address_id" to "idn_email",
        "primary_phone_number_id" to "idn_phone",
        "profile_image_id" to "img",
        "public_metadata" to """{"a":1}""",
        "private_metadata" to """{"b":2}""",
        "unsafe_metadata" to """{"c":3}""",
      ),
      User.UpdateParams(
          firstName = "Ada",
          lastName = "Lovelace",
          username = "ada",
          primaryEmailAddressId = "idn_email",
          primaryPhoneNumberId = "idn_phone",
          profileImageId = "img",
          publicMetadata = """{"a":1}""",
          privateMetadata = """{"b":2}""",
          unsafeMetadata = """{"c":3}""",
        )
        .toFormFields(),
    )
  }

  @Test
  fun `user metadata, password and external account params`() {
    assertEquals(emptyMap<String, String>(), User.UpdateMetadataParams().toFormFields())
    assertEquals(
      mapOf("unsafe_metadata" to """{"c":null}"""),
      User.UpdateMetadataParams(unsafeMetadata = """{"c":null}""").toFormFields(),
    )
    assertEquals(
      mapOf("new_password" to "new", "sign_out_of_other_sessions" to "false"),
      User.UpdatePasswordParams(newPassword = "new").toFormFields(),
    )
    assertEquals(
      mapOf(
        "current_password" to "old",
        "new_password" to "new",
        "sign_out_of_other_sessions" to "true",
      ),
      User.UpdatePasswordParams(
          currentPassword = "old",
          newPassword = "new",
          signOutOfOtherSessions = true,
        )
        .toFormFields(),
    )
    assertEquals(
      mapOf("strategy" to "oauth_github", "redirect_url" to "clerk://callback"),
      User.CreateExternalAccountParams(
          provider = OAuthProvider.GITHUB,
          redirectUrl = "clerk://callback",
        )
        .toFormFields(),
    )
    assertEquals(
      mapOf(
        "strategy" to "oauth_custom_patreon",
        "redirect_url" to "clerk://callback",
        "oidc_prompt" to "login",
        "oidc_login_hint" to "alice",
      ),
      User.CreateExternalAccountParams(
          provider = OAuthProvider.custom("oauth_custom_patreon"),
          redirectUrl = "clerk://callback",
          oidcPrompt = "login",
          oidcLoginHint = "alice",
        )
        .toFormFields(),
    )
  }

  @Test
  fun `email address verification preparation sends snake case keys`() {
    val fields =
      listOf<EmailAddress.PrepareVerificationParams>(
          EmailAddress.PrepareVerificationParams.EmailCode(),
          EmailAddress.PrepareVerificationParams.EnterpriseSSO(),
          EmailAddress.PrepareVerificationParams.EnterpriseSSO(
            redirectUrl = "clerk://callback",
            actionCompleteRedirectUrl = "clerk://done",
          ),
        )
        .map { it.toFormFields() }

    assertEquals(
      listOf(
        mapOf("strategy" to "email_code"),
        mapOf("strategy" to "enterprise_sso"),
        mapOf(
          "strategy" to "enterprise_sso",
          "redirect_url" to "clerk://callback",
          "action_complete_redirect_url" to "clerk://done",
        ),
      ),
      fields,
    )
  }

  @Test
  fun `session reverification params`() {
    assertEquals(
      mapOf("level" to "second_factor"),
      Session.StartVerificationParams("second_factor").toFormFields(),
    )
    assertEquals(
      mapOf("strategy" to "email_code"),
      Session.PrepareFirstFactorParams(strategy = "email_code").toFormFields(),
    )
    assertEquals(
      mapOf(
        "strategy" to "enterprise_sso",
        "email_address_id" to "idn_email",
        "phone_number_id" to "idn_phone",
        "enterprise_connection_id" to "conn",
        "redirect_url" to "clerk://callback",
        "trusted_device_id" to "td",
      ),
      Session.PrepareFirstFactorParams(
          strategy = "enterprise_sso",
          emailAddressId = "idn_email",
          phoneNumberId = "idn_phone",
          enterpriseConnectionId = "conn",
          redirectUrl = "clerk://callback",
          biometricCredentialId = "td",
        )
        .toFormFields(),
    )
    assertEquals(
      mapOf(
        "strategy" to "phone_code",
        "phone_number_id" to "idn_phone",
        "trusted_device_id" to "td",
      ),
      Session.PrepareSecondFactorParams(
          strategy = "phone_code",
          phoneNumberId = "idn_phone",
          biometricCredentialId = "td",
        )
        .toFormFields(),
    )
  }

  @Test
  fun `session factor attempts send code, password or credential`() {
    val firstFactor =
      listOf<Session.AttemptFirstFactorParams>(
          Session.AttemptFirstFactorParams.Password(password = "pw"),
          Session.AttemptFirstFactorParams.Passkey(publicKeyCredential = "cred"),
          Session.AttemptFirstFactorParams.Code(code = "424242", strategy = "email_code"),
          Session.AttemptFirstFactorParams.BiometricCredential(
            biometricCredentialId = "td",
            clientData = "data",
            signature = "sig",
            algorithm = "ES256",
          ),
        )
        .map { it.toFormFields() }
    val secondFactor =
      listOf<Session.AttemptSecondFactorParams>(
          Session.AttemptSecondFactorParams.Code(strategy = "totp", code = "424242"),
          Session.AttemptSecondFactorParams.Passkey(publicKeyCredential = "cred"),
          Session.AttemptSecondFactorParams.BiometricCredential(
            biometricCredentialId = "td",
            clientData = "data",
            signature = "sig",
            algorithm = "ES256",
          ),
        )
        .map { it.toFormFields() }
    val biometric =
      mapOf(
        "trusted_device_id" to "td",
        "client_data" to "data",
        "signature" to "sig",
        "algorithm" to "ES256",
        "strategy" to "trusted_device",
      )

    assertEquals(
      listOf(
        mapOf("password" to "pw", "strategy" to "password"),
        mapOf("public_key_credential" to "cred", "strategy" to "passkey"),
        mapOf("code" to "424242", "strategy" to "email_code"),
        biometric,
      ),
      firstFactor,
    )
    assertEquals(
      listOf(
        mapOf("strategy" to "totp", "code" to "424242"),
        mapOf("public_key_credential" to "cred", "strategy" to "passkey"),
        biometric,
      ),
      secondFactor,
    )
  }

  @Test
  fun `create params encode every subtype, including the ones sent by hand`() {
    listOf<SignIn.CreateParams.Strategy>(
        SignIn.CreateParams.Strategy.Transfer(),
        SignIn.CreateParams.Strategy.Passkey(),
        SignIn.CreateParams.Strategy.BiometricCredential(id = "cred"),
      )
      .forEach { it.toFormFields() }
    listOf(
        SignUp.CreateParams.None,
        SignUp.CreateParams.Transfer,
        SignUp.CreateParams.Ticket(ticket = "tkt"),
        SignUp.CreateParams.GoogleOneTap(token = "token"),
      )
      .forEach { sendSignUpCreate(it) }
  }

  @Test
  fun `redirect params send the OAuth provider as its strategy`() {
    val providers = listOf(OAuthProvider.GOOGLE, OAuthProvider.custom("oauth_custom_patreon"))

    val signIn = providers.map {
      SignIn.AuthenticateWithRedirectParams.OAuth(provider = it, redirectUrl = "clerk://callback")
        .toFormFields()["strategy"]
    }
    val signUp = providers.map {
      SignUp.AuthenticateWithRedirectParams.OAuth(provider = it).toFormFields()["strategy"]
    }

    assertEquals(listOf("oauth_google", "oauth_custom_patreon"), signIn)
    assertEquals(listOf("oauth_google", "oauth_custom_patreon"), signUp)
  }

  @Test
  fun `unsafe metadata decodes back to the plain values it was built from`() {
    val json = Json.encodeToString(SignUp.CreateParams.Standard(unsafeMetadata = NESTED_METADATA))

    val decoded = Json.decodeFromString<SignUp.CreateParams.Standard>(json).unsafeMetadata

    assertEquals(
      mapOf(
        "plan" to "pro",
        "seats" to 3L,
        "ratio" to 1.5,
        "beta" to true,
        "tags" to listOf("a", "b"),
        "nested" to mapOf("k" to "v", "gone" to null),
      ),
      decoded,
    )
  }

  private fun sendSignUpCreate(params: SignUp.CreateParams): Map<String, String> =
    params.toFormFields()

  private companion object {
    val NESTED_METADATA: Map<String, Any> =
      mapOf(
        "plan" to "pro",
        "seats" to 3,
        "ratio" to 1.5,
        "beta" to true,
        "tags" to listOf("a", "b"),
        "nested" to mapOf("k" to "v", "gone" to null),
      )
    const val NESTED_METADATA_JSON =
      """{"plan":"pro","seats":3,"ratio":1.5,"beta":true,"tags":["a","b"],""" +
        """"nested":{"k":"v","gone":null}}"""
  }
}
