package com.clerk.automap.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration

private const val LOG_LIMIT = 500

/** Generates toMap() extension functions for sealed interfaces. */
internal class SealedInterfaceGenerator(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
) {

  private val fileSuppressAnnotation =
    "@file:Suppress(\"REDUNDANT_ELSE_IN_WHEN\", \"USELESS_CAST\")"

  /**
   * Generates a toMap() extension function for a sealed interface that delegates to the toMap()
   * functions of its @AutoMap implementations.
   */
  fun generateSealedInterfaceExtension(
    sealedParent: KSClassDeclaration,
    implementations: List<KSClassDeclaration>,
  ) {
    val packageName = sealedParent.packageName.asString()
    val parentQualifiedName = sealedParent.qualifiedName?.asString() ?: return

    // Create a unique filename based on the full qualified name
    val uniqueIdentifier = parentQualifiedName.replace(".", "_").replace("$", "_")
    val filename = "${uniqueIdentifier}SealedMapExtension"

    logger.info("Generating sealed interface extension for $parentQualifiedName")
    logger.info("Implementations: ${implementations.map { it.qualifiedName?.asString() }}")
    logger.info("Filename: $filename")

    val file =
      codeGenerator.createNewFile(
        Dependencies(true, sealedParent.containingFile!!),
        packageName,
        filename,
      )

    // Get visibility modifier of the sealed parent
    val visibilityModifier = VisibilityHelper.visibilityModifierString(sealedParent)

    // Get the function parameter type name
    val functionParameterType = getNestedClassName(sealedParent)
    logger.info("Function parameter type: $functionParameterType")

    val content = buildString {
      appendLine(fileSuppressAnnotation)
      appendLine("package $packageName")
      appendLine()

      // Don't import anything - use fully qualified names to avoid conflicts

      appendLine()
      // Apply the same visibility modifier as the parent class
      appendLine("${visibilityModifier}fun $functionParameterType.toMap(): Map<String, String> {")
      appendLine("    return when (this) {")

      // Generate when branches ONLY for @AutoMap implementations using fully qualified names
      implementations.forEach { impl ->
        val implFullyQualifiedName = impl.qualifiedName?.asString() ?: return@forEach
        val implNestedName = getNestedClassName(impl)
        logger.info("Adding when branch for: $implNestedName (qualified: $implFullyQualifiedName)")
        appendLine("        is $implNestedName -> (this as $implNestedName).toMap()")
      }

      // Add an else branch for any non-@AutoMap implementations
      appendLine("        else -> emptyMap()")

      appendLine("    }")
      appendLine("}")
    }

    logger.info("Generated content preview:")
    logger.info(content.take(LOG_LIMIT))

    file.write(content.toByteArray())
    file.close()
  }

  /** Helper function to get the nested class name properly. */
  private fun getNestedClassName(classDeclaration: KSClassDeclaration): String {
    val containingDeclarations = mutableListOf<String>()
    var currentDeclaration: KSDeclaration? = classDeclaration

    while (currentDeclaration != null && currentDeclaration is KSClassDeclaration) {
      containingDeclarations.add(0, currentDeclaration.simpleName.asString())
      currentDeclaration = currentDeclaration.parentDeclaration
    }

    return containingDeclarations.joinToString(".")
  }
}
