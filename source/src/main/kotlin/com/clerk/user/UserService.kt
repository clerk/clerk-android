package com.clerk.user

import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.image.ImageResource
import com.clerk.network.serialization.ClerkResult
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Internal service object that provides user-related operations and API interactions.
 *
 * This service handles user profile management operations such as updating profile photos and other
 * user-specific functionality through the Clerk API.
 */
internal object UserService {

  /**
   * Sets the profile photo for a user by uploading an image file.
   *
   * This function takes an image file, converts it to a multipart request with JPEG media type, and
   * uploads it to the Clerk API to set as the user's profile photo. The file is given a random UUID
   * as the filename in the request.
   *
   * @param file The image file to upload as the profile photo. Should be a valid image file.
   * @return A [ClerkResult] containing either the uploaded [ImageResource] on success or a
   *   [ClerkErrorResponse] on failure.
   */
  suspend fun setProfilePhoto(file: File): ClerkResult<ImageResource, ClerkErrorResponse> {
    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("file", "filename.jpeg", requestFile)

    return ClerkApi.user.setProfileImage(file = body)
  }
}
