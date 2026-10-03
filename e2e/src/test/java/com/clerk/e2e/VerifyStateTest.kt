package com.clerk.e2e

import kotlin.test.Test
import kotlin.test.assertEquals

class VerifyStateTest {
  @Test
  fun `signed out line has sorted keys and explicit nulls`() {
    val state =
      VerifyState(
        runId = null,
        launchId = null,
        screen = HostScreen.Error,
        environmentLoaded = false,
        userId = null,
        sessionId = null,
        sessionStatus = null,
        pendingTasks = emptyList(),
        orgId = null,
        signInStatus = null,
        signUpStatus = null,
        ticket = VerifyTicket.Failed,
        lastError = VerifyFailure(code = "invalid_publishable_key", message = "bad \"key\""),
      )

    assertEquals(
      "verify {\"environmentLoaded\":false," +
        "\"lastError\":{\"code\":\"invalid_publishable_key\",\"message\":\"bad \\\"key\\\"\"}," +
        "\"launchId\":null,\"orgId\":null,\"pendingTasks\":[],\"runId\":null,\"screen\":\"error\"," +
        "\"sessionId\":null,\"sessionStatus\":null,\"signInStatus\":null,\"signUpStatus\":null," +
        "\"signedIn\":false,\"ticket\":\"failed\",\"userId\":null,\"v\":1}",
      state.line,
    )
  }

  @Test
  fun `signed in line reports the user and session`() {
    val state =
      VerifyState(
        runId = "run",
        launchId = "launch",
        screen = VerifyScreen.UserProfile,
        environmentLoaded = true,
        userId = "user_1",
        sessionId = "sess_1",
        sessionStatus = "pending",
        pendingTasks = listOf("choose-organization"),
        orgId = "org_1",
        signInStatus = "complete",
        signUpStatus = null,
        ticket = VerifyTicket.Succeeded,
        lastError = null,
      )

    assertEquals(
      "verify {\"environmentLoaded\":true,\"lastError\":null,\"launchId\":\"launch\"," +
        "\"orgId\":\"org_1\",\"pendingTasks\":[\"choose-organization\"],\"runId\":\"run\"," +
        "\"screen\":\"userProfile\",\"sessionId\":\"sess_1\",\"sessionStatus\":\"pending\"," +
        "\"signInStatus\":\"complete\",\"signUpStatus\":null,\"signedIn\":true," +
        "\"ticket\":\"succeeded\",\"userId\":\"user_1\",\"v\":1}",
      state.line,
    )
  }
}
