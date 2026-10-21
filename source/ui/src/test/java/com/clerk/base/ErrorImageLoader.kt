package com.clerk.base

import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult

// Coil finishes a real load on its own schedule, so Paparazzi may capture the image either still
// loading or already failed. Failing every request at once renders the error slot every time.
internal class ErrorImageLoader(delegate: ImageLoader) : ImageLoader by delegate {
  override suspend fun execute(request: ImageRequest): ImageResult =
    ErrorResult(
      image = null,
      request = request,
      throwable = IllegalStateException("Snapshot tests load no images"),
    )
}
