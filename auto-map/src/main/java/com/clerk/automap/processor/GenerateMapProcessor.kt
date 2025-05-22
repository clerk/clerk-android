@file:Suppress("NestedBlockDepth", "TooGenericExceptionCaught", "ReturnCount", "MagicNumber")

package com.clerk.automap.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Visibility
import com.google.devtools.ksp.validate

class GenerateMapProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
) : SymbolProcessor {

  // Collect sealed interfaces and their implementations
  private val sealedInterfacesMap = mutableMapOf<String, MutableList<KSClassDeclaration>>()
  private val fileSuppressAnnotation =
    "@file:Suppress(\"REDUNDANT_ELSE_IN_WHEN\", \"USELESS_CAST\")"

  // Track processed classes to avoid duplicates
  private val processedDataClasses = mutableSetOf<String>()
  private val processedSealedInterfaces = mutableSetOf<String>()

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver.getSymbolsWithAnnotation("com.clerk.automap.annotation.AutoMap")
    val unprocessed = symbols.filter { !it.validate() }.toList()

    try {
      // First pass: collect all implementations and their sealed parents
      symbols
        .filter { it is KSClassDeclaration && it.validate() }
        .forEach {
          val declaration = it as KSClassDeclaration
          if (declaration.isDataClass()) {
            // Check if this is part of a sealed hierarchy
            val sealedParent = findSealedParent(declaration)
            if (sealedParent != null) {
              val parentQualifiedName = sealedParent.qualifiedName?.asString() ?: return@forEach
              sealedInterfacesMap.getOrPut(parentQualifiedName) { mutableListOf() }.add(declaration)
            }

            // Process the data class as usual
            val qualifiedName = declaration.qualifiedName?.asString() ?: return@forEach
            if (qualifiedName !in processedDataClasses) {
              declaration.accept(MapVisitor(), Unit)
              processedDataClasses.add(qualifiedName)
            }
          }
        }

      // Second pass: generate extensions for sealed interfaces (once per sealed interface)
      sealedInterfacesMap.forEach { (qualifiedName, implementations) ->
        if (qualifiedName !in processedSealedInterfaces) {
          val firstImplementation = implementations.firstOrNull() ?: return@forEach
          val sealedParent = findSealedParent(firstImplementation) ?: return@forEach
          generateSealedInterfaceExtension(sealedParent, implementations)
          processedSealedInterfaces.add(qualifiedName)
        }
      }
    } catch (e: Exception) {
      logger.error("Error in processor: ${e.message}")
    }

    return unprocessed
  }

  private fun findSealedParent(classDeclaration: KSClassDeclaration): KSClassDeclaration? {
    // Get all superTypes of the class declaration
    return classDeclaration.superTypes
      // Resolve each type and get its declaration
      .mapNotNull { it.resolve().declaration as? KSClassDeclaration }
      // Find the first that is a sealed class or interface
      .firstOrNull { it.modifiers.contains(Modifier.SEALED) }
  }

  // Helper function to determine the most restrictive visibility across
  // class and all its parent classes/interfaces
  private fun getEffectiveVisibility(classDeclaration: KSClassDeclaration): Visibility {
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

  // Helper to determine if one visibility is more restrictive than another
  private fun isMoreRestrictive(v1: Visibility, v2: Visibility): Boolean {
    val order =
      mapOf(
        Visibility.PRIVATE to 1,
        Visibility.INTERNAL to 2,
        Visibility.PROTECTED to 3,
        Visibility.PUBLIC to 4,
      )
    return (order[v1] ?: 4) < (order[v2] ?: 4)
  }

  // Generate the visibility modifier string based on effective visibility
  private fun visibilityModifierString(classDeclaration: KSClassDeclaration): String {
    return when (getEffectiveVisibility(classDeclaration)) {
      Visibility.INTERNAL -> "internal "
      Visibility.PRIVATE ->
        "private " // Note: Extensions can't be more restrictive than what they're extending
      Visibility.PROTECTED -> "protected " // Note: Usually doesn't apply to extensions
      else -> "" // PUBLIC is default
    }
  }

  private fun generateSealedInterfaceExtension(
    sealedParent: KSClassDeclaration,
    implementations: List<KSClassDeclaration>,
  ) {
    val packageName = sealedParent.packageName.asString()
    val parentQualifiedName = sealedParent.qualifiedName?.asString() ?: return
    val className = sealedParent.simpleName.asString()

    // Skip if we've already processed this sealed interface
    if (parentQualifiedName in processedSealedInterfaces) {
      return
    }

    // Use a distinct filename to avoid conflicts
    val filename = "${className}SealedMapExtension"

    logger.info("Generating sealed interface extension for $parentQualifiedName")

    // Collect all necessary imports
    val importedClasses = mutableSetOf<String>()
    importedClasses.add(parentQualifiedName)

    // Add implementations to imports
    implementations.forEach { impl ->
      val implQualifiedName = impl.qualifiedName?.asString() ?: return@forEach
      importedClasses.add(implQualifiedName)
    }

    val file =
      codeGenerator.createNewFile(
        Dependencies(true, sealedParent.containingFile!!),
        packageName,
        filename,
      )

    // Get visibility modifier of the sealed parent
    val visibilityModifier = visibilityModifierString(sealedParent)

    val content = buildString {
      appendLine(fileSuppressAnnotation)
      appendLine("package $packageName")
      appendLine()

      // Add imports
      importedClasses.forEach { qualifiedName -> appendLine("import $qualifiedName") }

      appendLine()
      // Apply the same visibility modifier as the parent class
      appendLine("${visibilityModifier}fun $className.toMap(): Map<String, String> {")
      appendLine("    return when (this) {")

      // Generate when branches for each implementation
      implementations.forEach { impl ->
        val implClassName = impl.simpleName.asString()
        appendLine("        is $implClassName -> (this as $implClassName).toMap()")
      }

      // Add an else branch for any non-@AutoMap implementations
      appendLine("        else -> emptyMap()")

      appendLine("    }")
      appendLine("}")
    }

    file.write(content.toByteArray())
    file.close()
  }

  inner class MapVisitor : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      super.visitClassDeclaration(classDeclaration, data)
      if (!classDeclaration.isDataClass()) {
        logger.error("@AutoMap can only be applied to data classes", classDeclaration)
        return
      }

      val packageName = classDeclaration.packageName.asString()
      val qualifiedName = classDeclaration.qualifiedName?.asString() ?: return
      val className = classDeclaration.simpleName.asString()

      // Skip if already processed
      if (qualifiedName in processedDataClasses) {
        return
      }

      // Handle nested classes properly
      val nestedPath = determineNestedPath(classDeclaration)
      val importPath = qualifiedName

      val properties = classDeclaration.getAllProperties().toList()

      val file =
        codeGenerator.createNewFile(
          Dependencies(true, classDeclaration.containingFile!!),
          packageName,
          className + "MapExtension",
        )

      file.write(
        generateExtension(packageName, nestedPath, importPath, properties, classDeclaration)
      )
      file.close()

      // Mark as processed
      processedDataClasses.add(qualifiedName)
    }
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
    val visibilityModifier = visibilityModifierString(classDeclaration)

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

  private fun KSClassDeclaration.isDataClass(): Boolean {
    return modifiers.contains(Modifier.DATA)
  }
}
