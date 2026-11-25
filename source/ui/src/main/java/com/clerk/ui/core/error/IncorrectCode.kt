package com.clerk.ui.core.error

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult

private const val FORM_CODE_INCORRECT = "form_code_incorrect"

internal val ClerkResult.Failure<ClerkErrorResponse>.isIncorrectCode: Boolean
  get() = error?.errors.orEmpty().any { it.code == FORM_CODE_INCORRECT }
