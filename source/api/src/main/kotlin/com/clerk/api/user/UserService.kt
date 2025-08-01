package com.clerk.api.user

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.clerk.api.Constants.Config.COMPRESSION_PERCENTAGE
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.image.ImageResource
import com.clerk.api.network.serialization.ClerkResult
import java.io.File
import java.io.FileOutputStream
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
    val compressedFile = compressImage(file)
    val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

    return ClerkApi.user.setProfileImage(body)
  }

  fun compressImage(file: File, quality: Int = COMPRESSION_PERCENTAGE): File {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    val outputFile = File(file.parent, "compressed_${file.name}")

    FileOutputStream(outputFile).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    }

    return outputFile
  }
}
