package com.clerk.mapgenerator.annotation

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
      val className = classDeclaration.simpleName.asString()

      val properties = classDeclaration.getAllProperties().toList()

      val file =
        codeGenerator.createNewFile(
          Dependencies(true, classDeclaration.containingFile!!),
          packageName,
          className + "MapExtension",
        )

      file.write(generateExtension(packageName, className, properties))
      file.close()
    }
  }

  private fun generateExtension(
    packageName: String,
    className: String,
    properties: List<KSPropertyDeclaration>,
  ): ByteArray {
    val content = buildString {
      appendLine("package $packageName")
      appendLine()
      appendLine("fun $className.toMap(): Map<String, Any?> {")
      appendLine("    return mapOf(")

      properties.forEachIndexed { index, property ->
        val propName = property.simpleName.asString()
        append("        \"$propName\" to this.$propName")
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

  class GenerateMapProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
      return GenerateMapProcessor(environment.codeGenerator, environment.logger)
    }
  }
}
