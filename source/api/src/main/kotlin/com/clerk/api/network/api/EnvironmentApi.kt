package com.clerk.api.network.api

import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.serialization.ClerkResult
import retrofit2.http.GET

/**
 * Internal API interface for environment configuration operations.
 *
 * This interface defines the REST API endpoints for retrieving environment configuration from the
 * Clerk system. The environment contains application-specific settings, display configuration,
 * authentication options, and feature flags that control SDK behavior.
 *
 * This is an internal API interface used by the Clerk SDK and should not be used directly by
 * application code.
 */
internal interface EnvironmentApi {
  /**
   * Retrieves the current environment configuration.
   *
   * This method fetches the complete environment configuration from the Clerk API, which includes
   * display settings, authentication configuration, enabled features, and other
   * environment-specific data that controls how the SDK behaves for this particular application.
   *
   * @return A [ClerkResult] containing the [Environment] object on success, or a
   *   [ClerkErrorResponse] on failure
   */
  @GET(ApiPaths.ENVIRONMENT) suspend fun get(): ClerkResult<Environment, ClerkErrorResponse>
}
