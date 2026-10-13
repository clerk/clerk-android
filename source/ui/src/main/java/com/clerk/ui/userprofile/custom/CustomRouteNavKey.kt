package com.clerk.ui.userprofile.custom

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable internal data class CustomRouteNavKey(val routeKey: String) : NavKey
