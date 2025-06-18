package com.clerk.network.middleware.incoming

import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import okhttp3.Interceptor
import okhttp3.Response

private const val REQUIRES_ASSERTION = "requires_assertion"

class DeviceAssertionMiddleware: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if(!response.isSuccessful) {
            val responseBody = response.body
            responseBody?.let { body ->
                try {
                    val responseString = body.string()
                    val errorResponse = ClerkApi.json.decodeFromString<ClerkErrorResponse>(responseString)
                    if(errorResponse.errors.any { it.code == REQUIRES_ASSERTION } == true) {

                    }
                }
            }
        }
    }
}