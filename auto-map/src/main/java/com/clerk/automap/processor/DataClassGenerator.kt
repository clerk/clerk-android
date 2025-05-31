package com.clerk.automap.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/** Generates toMap() extension functions for individual data classes. */
internal class DataClassGenerator(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
) {

  /** Generates a toMap() extension function for a data class. */
  fun generateDataClassExtension(classDeclaration: KSClassDeclaration) {
    val packageName = classDeclaration.packageName.asString()
    val className = classDeclaration.simpleName.asString()

    // Handle nested classes properly
    val nestedPath = determineNestedPath(classDeclaration)
    val importPath = classDeclaration.qualifiedName?.asString() ?: return

    val properties = classDeclaration.getAllProperties().toList()

    val file =
      codeGenerator.createNewFile(
        Dependencies(true, classDeclaration.containingFile!!),
        packageName,
        className + "MapExtension",
      )

    file.write(generateExtension(packageName, nestedPath, importPath, properties, classDeclaration))
    file.close()
  }

  private fun determineNestedPath(classDeclaration: KSClassDeclaration): String {
    val containingDeclarations = mutableListOf<String>()
    var currentDeclaration: KSDeclaration? = classDeclaration

    while (currentDeclaration != null && currentDeclaration is KSClassDeclaration) {
      containingDeclarations.add(0, currentDeclaration.simpleName.asString())
      currentDeclaration = currentDeclaration.parentDeclaration
    }

    return containingDeclarations.joinToString(".")
  }

  private fun generateExtension(
    packageName: String,
    nestedPath: String,
    importPath: String,
    properties: List<KSPropertyDeclaration>,
    classDeclaration: KSClassDeclaration,
  ): ByteArray {
    // Get visibility modifier of the class or parent interfaces if more restrictive
    val visibilityModifier = VisibilityHelper.visibilityModifierString(classDeclaration)

    val content = buildString {
      appendLine("package $packageName")
      appendLine()
      appendLine("import $importPath")
      appendLine("import kotlinx.serialization.SerialName")
      appendLine()
      // Apply the appropriate visibility modifier
      appendLine("${visibilityModifier}fun $nestedPath.toMap(): Map<String, String> {")
      appendLine("    val map = mutableMapOf<String, String>()")

      // Instead of directly creating the map, we'll build it conditionally
      logger.info("Processing class: ${classDeclaration.qualifiedName?.asString()}")
      logger.info("Properties found:")
      properties.forEach { property ->
        val propName = property.simpleName.asString()

        // Check for @SerialName annotation
        val serialNameAnnotation =
          property.annotations.find {
            it.shortName.asString() == "SerialName" &&
              it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                "kotlinx.serialization.SerialName"
          }

        val keyName =
          if (serialNameAnnotation != null) {
            val serialNameValue =
              serialNameAnnotation.arguments
                .find { it.name?.asString() == null || it.name?.asString() == "value" }
                ?.value
                ?.toString()
                ?.trim('"') ?: propName
            serialNameValue
          } else {
            propName
          }

        // Add null check before adding to map
        appendLine("    this.$propName?.let { map[\"$keyName\"] = it.toString() }")
      }

      appendLine("    return map")
      appendLine("}")
    }
    return content.toByteArray()
  }
}
