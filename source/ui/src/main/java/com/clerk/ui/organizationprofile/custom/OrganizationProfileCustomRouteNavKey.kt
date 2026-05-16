package com.clerk.ui.organizationprofile.custom

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** A [NavKey] that carries a string route key for custom organization profile destinations. */
@Serializable
internal data class OrganizationProfileCustomRouteNavKey(val routeKey: String) : NavKey
