package com.clerk.mapgenerator.processor

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
import com.google.devtools.ksp.validate

class GenerateMapProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
) : SymbolProcessor {
  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver.getSymbolsWithAnnotation("com.clerk.mapgenerator.annotation.GenerateMap")
    val unprocessed = symbols.filter { !it.validate() }.toList()

    symbols
      .filter { it is KSClassDeclaration && it.validate() }
      .forEach { it.accept(MapVisitor(), Unit) }

    return unprocessed
  }

  inner class MapVisitor : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      super.visitClassDeclaration(classDeclaration, data)
      if (!classDeclaration.isDataClass()) {
        logger.error("@GenerateMap can only be applied to data classes", classDeclaration)
        return
      }

      val packageName = classDeclaration.packageName.asString()
      val qualifiedName = classDeclaration.qualifiedName?.asString() ?: return
      val className = classDeclaration.simpleName.asString()

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

      file.write(generateExtension(packageName, nestedPath, importPath, properties))
      file.close()
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
  ): ByteArray {
    val content = buildString {
      appendLine("package $packageName")
      appendLine()
      appendLine("import $importPath")
      appendLine("import kotlinx.serialization.SerialName")
      appendLine()
      appendLine("fun $nestedPath.toMap(): Map<String, Any?> {")
      appendLine("    return mapOf(")

      properties.forEachIndexed { index, property ->
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

        append("        \"$keyName\" to this.$propName")
        if (index < properties.size - 1) {
          appendLine(",")
        } else {
          appendLine()
        }
      }

      appendLine("    )")
      appendLine("}")
    }
    return content.toByteArray()
  }

  private fun KSClassDeclaration.isDataClass(): Boolean {
    return modifiers.contains(Modifier.DATA)
  }
}
