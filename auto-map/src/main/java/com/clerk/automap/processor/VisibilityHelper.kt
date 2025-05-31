package com.clerk.automap.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Visibility

/** Helper class for handling visibility modifiers in generated extensions. */
internal object VisibilityHelper {

  private const val PRIVATE_VISIBILITY_ORDER = 1
  private const val INTERNAL_VISIBILITY_ORDER = 2
  private const val PROTECTED_VISIBILITY_ORDER = 3
  private const val PUBLIC_VISIBILITY_ORDER = 4

  /**
   * Determines the most restrictive visibility across class and all its parent classes/interfaces.
   */
  fun getEffectiveVisibility(classDeclaration: KSClassDeclaration): Visibility {
    // Start with the class's own visibility
    var mostRestrictive = classDeclaration.getVisibility()

    // Get parent class and interfaces
    val superTypes =
      classDeclaration.superTypes.mapNotNull { it.resolve().declaration as? KSClassDeclaration }

    // Find the most restrictive visibility among parent classes
    for (superType in superTypes) {
      val parentVisibility = superType.getVisibility()
      // If parent is more restrictive, use its visibility
      if (isMoreRestrictive(parentVisibility, mostRestrictive)) {
        mostRestrictive = parentVisibility
      }
    }

    return mostRestrictive
  }

  /** Generates the visibility modifier string based on effective visibility. */
  fun visibilityModifierString(classDeclaration: KSClassDeclaration): String {
    return when (getEffectiveVisibility(classDeclaration)) {
      Visibility.INTERNAL -> "internal "
      Visibility.PRIVATE ->
        "private " // Note: Extensions can't be more restrictive than what they're extending
      Visibility.PROTECTED -> "protected " // Note: Usually doesn't apply to extensions
      else -> "" // PUBLIC is default
    }
  }

  /** Determines if one visibility is more restrictive than another. */
  private fun isMoreRestrictive(v1: Visibility, v2: Visibility): Boolean {
    val order =
      mapOf(
        Visibility.PRIVATE to PRIVATE_VISIBILITY_ORDER,
        Visibility.INTERNAL to INTERNAL_VISIBILITY_ORDER,
        Visibility.PROTECTED to PROTECTED_VISIBILITY_ORDER,
        Visibility.PUBLIC to PUBLIC_VISIBILITY_ORDER,
      )
    return (order[v1] ?: PUBLIC_VISIBILITY_ORDER) < (order[v2] ?: PUBLIC_VISIBILITY_ORDER)
  }
}
