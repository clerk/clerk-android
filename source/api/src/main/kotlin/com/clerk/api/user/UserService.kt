package com.clerk.api.user

import com.clerk.api.image.ImageService
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.image.ImageResource
import com.clerk.api.network.serialization.ClerkResult
import java.io.File

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
    val body = ImageService().createMultipartBody(file)
    return ClerkApi.user.setProfileImage(body)
  }
}
