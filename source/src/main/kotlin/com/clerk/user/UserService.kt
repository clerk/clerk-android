package com.clerk.user

import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.image.ImageResource
import com.clerk.network.serialization.ClerkResult
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

internal object UserService {

  suspend fun setProfilePhoto(
    sessionId: String? = null,
    file: File,
  ): ClerkResult<ImageResource, ClerkErrorResponse> {
    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

    return ClerkApi.user.setProfileImage(sessionId, body)
  }
}
