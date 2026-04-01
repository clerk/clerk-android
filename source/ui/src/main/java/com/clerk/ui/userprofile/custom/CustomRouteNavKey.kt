package com.clerk.ui.userprofile.custom

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** A [NavKey] that carries a string route key for custom user profile destinations. */
@Serializable
internal data class CustomRouteNavKey(val routeKey: String) : NavKey
