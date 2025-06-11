package com.clerk.network.serialization

internal class ApiException(public val error: Any?) : Exception()
