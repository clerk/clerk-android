@file:Suppress("TooGenericExceptionCaught", "ReturnCount")

package com.clerk.automap.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate

/** Main processor that orchestrates the generation of toMap() extensions for @AutoMap classes. */
class GenerateMapProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
) : SymbolProcessor {

  // Collect sealed interfaces and their implementations
  private val sealedInterfacesMap = mutableMapOf<String, MutableList<KSClassDeclaration>>()

  // Track processed classes to avoid duplicates
  private val processedDataClasses = mutableSetOf<String>()
  private val processedSealedInterfaces = mutableSetOf<String>()

  // Generators
  private val sealedInterfaceGenerator = SealedInterfaceGenerator(codeGenerator, logger)
  private val dataClassGenerator = DataClassGenerator(codeGenerator, logger)

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver.getSymbolsWithAnnotation("com.clerk.automap.annotation.AutoMap")
    val unprocessed = symbols.filter { !it.validate() }.toList()

    try {
      val sealedImplementations = collectSealedImplementations(symbols)
      processStandaloneDataClasses(symbols, sealedImplementations)
      generateSealedInterfaceExtensions()
    } catch (e: Exception) {
      logger.error("Error in processor: ${e.message}")
      logger.error("Stack trace: ${e.stackTraceToString()}")
    }

    return unprocessed
  }

  private fun collectSealedImplementations(symbols: Sequence<KSAnnotated>): Set<String> {
    val sealedImplementations = mutableSetOf<String>()

    symbols
      .filterIsInstance<KSClassDeclaration>()
      .filter { it.validate() && it.isDataClass() }
      .forEach { declaration ->
        val sealedParent = findSealedParent(declaration)
        if (sealedParent != null) {
          addToSealedInterfaceMap(declaration, sealedParent, sealedImplementations)
        }
      }

    return sealedImplementations
  }

  private fun addToSealedInterfaceMap(
    declaration: KSClassDeclaration,
    sealedParent: KSClassDeclaration,
    sealedImplementations: MutableSet<String>,
  ) {
    val parentQualifiedName = sealedParent.qualifiedName?.asString() ?: return
    val childQualifiedName = declaration.qualifiedName?.asString() ?: return

    sealedInterfacesMap.getOrPut(parentQualifiedName) { mutableListOf() }.add(declaration)
    sealedImplementations.add(childQualifiedName)
  }

  private fun processStandaloneDataClasses(
    symbols: Sequence<KSAnnotated>,
    sealedImplementations: Set<String>,
  ) {
    symbols
      .filterIsInstance<KSClassDeclaration>()
      .filter { it.validate() && it.isDataClass() }
      .forEach { declaration ->
        val qualifiedName = declaration.qualifiedName?.asString() ?: return@forEach

        if (shouldProcessStandaloneClass(qualifiedName, sealedImplementations)) {
          declaration.accept(MapVisitor(), Unit)
          processedDataClasses.add(qualifiedName)
        }
      }
  }

  private fun shouldProcessStandaloneClass(
    qualifiedName: String,
    sealedImplementations: Set<String>,
  ): Boolean {
    return qualifiedName !in sealedImplementations && qualifiedName !in processedDataClasses
  }

  private fun generateSealedInterfaceExtensions() {
    sealedInterfacesMap.forEach { (qualifiedName, implementations) ->
      if (qualifiedName !in processedSealedInterfaces) {
        val firstImplementation = implementations.firstOrNull() ?: return@forEach
        val sealedParent = findSealedParent(firstImplementation) ?: return@forEach
        sealedInterfaceGenerator.generateSealedInterfaceExtension(sealedParent, implementations)
        processedSealedInterfaces.add(qualifiedName)
      }
    }
  }

  private fun findSealedParent(classDeclaration: KSClassDeclaration): KSClassDeclaration? {
    // Get all superTypes of the class declaration
    return classDeclaration.superTypes
      // Resolve each type and get its declaration
      .mapNotNull { it.resolve().declaration as? KSClassDeclaration }
      // Find the first that is a sealed class or interface
      .firstOrNull { it.modifiers.contains(Modifier.SEALED) }
  }

  inner class MapVisitor : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      super.visitClassDeclaration(classDeclaration, data)
      if (!classDeclaration.isDataClass()) {
        logger.error("@AutoMap can only be applied to data classes", classDeclaration)
        return
      }

      val qualifiedName = classDeclaration.qualifiedName?.asString() ?: return

      // Skip if already processed
      if (qualifiedName in processedDataClasses) {
        return
      }

      dataClassGenerator.generateDataClassExtension(classDeclaration)

      // Mark as processed
      processedDataClasses.add(qualifiedName)
    }
  }

  private fun KSClassDeclaration.isDataClass(): Boolean {
    return modifiers.contains(Modifier.DATA)
  }
}
