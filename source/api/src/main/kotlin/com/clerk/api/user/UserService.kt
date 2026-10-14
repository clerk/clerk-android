package com.clerk.api.user

import com.clerk.api.image.ImageService
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.image.ImageResource
import com.clerk.api.network.serialization.ClerkResult
import java.io.File

internal object UserService {
  suspend fun setProfilePhoto(file: File): ClerkResult<ImageResource, ClerkErrorResponse> {
    val body = ImageService().createMultipartBody(file)
    return ClerkApi.user.setProfileImage(body)
  }
}
