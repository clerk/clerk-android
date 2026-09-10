package com.clerk.api

import androidx.credentials.GetPublicKeyCredentialOption
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class PasskeyRequestTest {
  @Test fun malformedCredentialIDsFailBeforeCredentialManagerPresentation() {
    for (encoded in listOf("", "!!!")) {
      val arguments = buildJsonObject {
        put("rpId", "example.com")
        put("challenge", buildJsonObject { put("base64url", "AQID") })
        put("allowCredentials", buildJsonArray {
          add(buildJsonObject {
            put("type", "public-key")
            put("id", buildJsonObject { put("base64url", encoded) })
          })
        })
      }
      val error = assertThrows(CoreException::class.java) { AndroidPasskeyRequests.get(arguments) }
      assertEquals("invalid_credential_options", error.code)
    }
  }

  @Test fun assertionRequestUsesCredentialManagerWebAuthnEncodingAndPromptPreference() {
    val request = AndroidPasskeyRequests.get(Json.parseToJsonElement("""{
      "challenge":{"base64url":"AQID"},"rpId":"example.com","timeout":60000,
      "allowCredentials":[{"type":"public-key","id":{"base64url":"BAUG"}}],
      "conditionalUI":false,"preferImmediatelyAvailableCredentials":true
    }"""))
    assertTrue(request.preferImmediatelyAvailableCredentials)
    val option = request.credentialOptions.single() as GetPublicKeyCredentialOption
    val webAuthn = Json.parseToJsonElement(option.requestJson).jsonObject
    assertEquals(JsonPrimitive("AQID"), webAuthn["challenge"])
    assertEquals(JsonPrimitive("BAUG"), webAuthn.getValue("allowCredentials").jsonArray.single().jsonObject["id"])
    assertEquals(JsonPrimitive("example.com"), webAuthn["rpId"])
    assertEquals(JsonPrimitive(60000), webAuthn["timeout"])
    assertFalse(webAuthn.containsKey("conditionalUI"))
    assertFalse(webAuthn.containsKey("preferImmediatelyAvailableCredentials"))
  }

  @Test fun registrationRequestEncodesUserHandleAndExcludedCredentialsForAndroid() {
    val request = AndroidPasskeyRequests.create(Json.parseToJsonElement("""{
      "challenge":{"base64url":"AQID"},"rp":{"id":"example.com","name":"Example"},
      "user":{"id":{"base64url":"BAUG"},"name":"person@example.com","displayName":"Person"},
      "pubKeyCredParams":[{"type":"public-key","alg":-7}],
      "excludeCredentials":[{"type":"public-key","id":{"base64url":"BwgJ"}}]
    }"""))
    val webAuthn = Json.parseToJsonElement(request.requestJson).jsonObject
    assertEquals(JsonPrimitive("AQID"), webAuthn["challenge"])
    assertEquals(JsonPrimitive("BAUG"), webAuthn.getValue("user").jsonObject["id"])
    assertEquals(JsonPrimitive("BwgJ"), webAuthn.getValue("excludeCredentials").jsonArray.single().jsonObject["id"])
    assertEquals(JsonPrimitive(-7), webAuthn.getValue("pubKeyCredParams").jsonArray.single().jsonObject["alg"])
  }
}
