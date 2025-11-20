package com.clerk.api.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.clerk.api.Constants.Config.COMPRESSION_PERCENTAGE
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/** Service for handling image operations including compression and multipart body creation. */
internal class ImageService {

  /**
   * Creates a multipart body part from an image file for HTTP requests. The image is compressed
   * before being converted to a multipart body.
   *
   * @param file The image file to convert to a multipart body
   * @return A MultipartBody.Part containing the compressed image data
   */
  fun createMultipartBody(file: File): MultipartBody.Part {
    val compressedFile = compressImage(file)
    val requestFile = compressedFile.asRequestBody("image/png".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("file", file.name, requestFile)
  }

  /**
   * Compresses an image file to reduce its size while maintaining visual quality. The compressed
   * image is saved as a new JPEG file with a "compressed_" prefix.
   *
   * @param file The original image file to compress
   * @param quality The compression quality percentage (default uses COMPRESSION_PERCENTAGE
   *   constant)
   * @return A new File containing the compressed image
   */
  private fun compressImage(file: File, quality: Int = COMPRESSION_PERCENTAGE): File {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    val outputFile = File(file.parent, "compressed_${file.name}")

    FileOutputStream(outputFile).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    }

    return outputFile
  }
}
