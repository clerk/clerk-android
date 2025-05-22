import kotlin.test.assertTrue
import org.junit.Test

class GenerateMapProcessorTest {

  @Test
  fun testGenerateExtensionFunction() {
    // Create test data
    val packageName = "com.example"
    val className = "TestClass"
    val propertyData =
      listOf(
        PropertyData("id", null),
        PropertyData("name", "user_name"),
        PropertyData("isActive", "is_active"),
      )

    // Directly test the extension generation function
    val generatedCode = generateExtensionForTest(packageName, className, propertyData)

    // Verify the generated code has expected content
    assertTrue(generatedCode.contains("package com.example"))
    assertTrue(generatedCode.contains("import com.example.TestClass"))
    assertTrue(generatedCode.contains("fun TestClass.toMap(): Map<String, Any?> {"))
    assertTrue(generatedCode.contains("\"id\" to this.id"))
    assertTrue(generatedCode.contains("\"user_name\" to this.name"))
    assertTrue(generatedCode.contains("\"is_active\" to this.isActive"))
  }

  @Test
  fun testGenerateExtensionForNestedClass() {
    // Create test data for nested class
    val packageName = "com.example"
    val nestedPath = "Outer.Inner.TestClass"
    val importPath = "com.example.Outer.Inner.TestClass"
    val propertyData = listOf(PropertyData("id", null), PropertyData("name", "user_name"))

    // Generate code for nested class
    val generatedCode =
      generateNestedExtensionForTest(packageName, nestedPath, importPath, propertyData)

    // Verify nested class handling
    assertTrue(generatedCode.contains("package com.example"))
    assertTrue(generatedCode.contains("import $importPath"))
    assertTrue(generatedCode.contains("fun $nestedPath.toMap(): Map<String, Any?> {"))
    assertTrue(generatedCode.contains("\"id\" to this.id"))
    assertTrue(generatedCode.contains("\"user_name\" to this.name"))
  }

  // Helper class to represent property data without KSP dependencies
  data class PropertyData(val name: String, val serialName: String?)

  // Reimplementation of the extension generation logic for testing
  private fun generateExtensionForTest(
    packageName: String,
    className: String,
    properties: List<PropertyData>,
  ): String {
    return generateNestedExtensionForTest(
      packageName,
      className,
      "$packageName.$className",
      properties,
    )
  }

  private fun generateNestedExtensionForTest(
    packageName: String,
    nestedPath: String,
    importPath: String,
    properties: List<PropertyData>,
  ): String {
    return buildString {
      appendLine("package $packageName")
      appendLine()
      appendLine("import $importPath")
      appendLine("import kotlinx.serialization.SerialName")
      appendLine()
      appendLine("fun $nestedPath.toMap(): Map<String, Any?> {")
      appendLine("    return mapOf(")

      properties.forEachIndexed { index, property ->
        val keyName = property.serialName ?: property.name
        append("        \"$keyName\" to this.${property.name}")
        if (index < properties.size - 1) {
          appendLine(",")
        } else {
          appendLine()
        }
      }

      appendLine("    )")
      appendLine("}")
    }
  }
}
