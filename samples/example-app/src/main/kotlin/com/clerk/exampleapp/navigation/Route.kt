package com.clerk.exampleapp.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
  @Serializable data object Loading : Route

  @Serializable data object Home : Route

  @Serializable data object SignIn : Route

  @Serializable data object SignUp : Route
}
