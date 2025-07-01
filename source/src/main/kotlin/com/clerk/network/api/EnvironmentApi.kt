package com.clerk.network.api

import com.clerk.network.model.environment.Environment
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.paths.Paths
import com.clerk.network.serialization.ClerkResult
import retrofit2.http.GET

/**
 * Internal API interface for environment configuration operations.
 *
 * This interface defines the REST API endpoints for retrieving environment configuration
 * from the Clerk system. The environment contains application-specific settings, display
 * configuration, authentication options, and feature flags that control SDK behavior.
 *
 * This is an internal API interface used by the Clerk SDK and should not be used directly
 * by application code.
 */
internal interface EnvironmentApi {
  /**
   * Retrieves the current environment configuration.
   *
   * This method fetches the complete environment configuration from the Clerk API, which includes
   * display settings, authentication configuration, enabled features, and other environment-specific
   * data that controls how the SDK behaves for this particular application.
   *
   * @return A [ClerkResult] containing the [Environment] object on success, or a [ClerkErrorResponse] on failure
   */
  @GET(Paths.ENVIRONMENT) suspend fun get(): ClerkResult<Environment, ClerkErrorResponse>
}
