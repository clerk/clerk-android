package com.clerk.api.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.clerk.api.Constants.Config.COMPRESSION_PERCENTAGE
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

internal class ImageService {
  fun createMultipartBody(file: File): MultipartBody.Part {
    val compressedFile = compressImage(file)
    val requestFile = compressedFile.asRequestBody("image/png".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("file", file.name, requestFile)
  }

  private fun compressImage(file: File, quality: Int = COMPRESSION_PERCENTAGE): File {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    val outputFile = File(file.parent, "compressed_${file.name}")

    FileOutputStream(outputFile).use { out ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    }

    return outputFile
  }
}
