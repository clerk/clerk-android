package com.clerk.gradle

import kotlin.test.Test
import kotlin.test.assertEquals

class DependencyRakeTest {
  private val notations = Notations(mapOf("androidx.test:core" to listOf("libs.androidx.core")))

  @Test
  fun `declares a used dependency that only the removed one provided`() {
    val plan =
      rake(
        advice =
          listOf(
            removal("testImplementation", "androidx.test:core-ktx"),
            addition("testImplementation", "androidx.test:core"),
          ),
        edges =
          mapOf(
            ROOT to setOf("androidx.test:core-ktx"),
            "androidx.test:core-ktx" to setOf("androidx.test:core"),
          ),
        declarations = mapOf("testImplementation" to setOf("androidx.test:core-ktx")),
      )

    assertEquals(listOf("androidx.test:core-ktx"), plan.removals.map { it.id })
    assertEquals(listOf("androidx.test:core"), plan.additions.map { it.id })
  }

  @Test
  fun `skips the addition when another declaration still provides the dependency`() {
    val plan =
      rake(
        advice =
          listOf(
            removal("testImplementation", "androidx.test:core-ktx"),
            addition("testImplementation", "androidx.test:core"),
          ),
        edges =
          mapOf(
            ROOT to setOf("androidx.test:core-ktx", "org.robolectric:robolectric"),
            "androidx.test:core-ktx" to setOf("androidx.test:core"),
            "org.robolectric:robolectric" to setOf("androidx.test:core"),
          ),
        declarations =
          mapOf(
            "testImplementation" to setOf("androidx.test:core-ktx", "org.robolectric:robolectric")
          ),
      )

    assertEquals(listOf("androidx.test:core-ktx"), plan.removals.map { it.id })
    assertEquals(emptyList(), plan.additions)
  }

  @Test
  fun `keeps a removal that provides a dependency with no catalog entry`() {
    val plan =
      rake(
        advice =
          listOf(
            removal("implementation", "org.jetbrains.compose.material:material-icons-core"),
            addition("implementation", "androidx.compose.material:material-icons-core"),
          ),
        edges =
          mapOf(
            ROOT to setOf("org.jetbrains.compose.material:material-icons-core"),
            "org.jetbrains.compose.material:material-icons-core" to
              setOf("androidx.compose.material:material-icons-core"),
          ),
        declarations =
          mapOf("implementation" to setOf("org.jetbrains.compose.material:material-icons-core")),
      )

    assertEquals(emptyList(), plan.removals)
    assertEquals(emptyList(), plan.additions)
    assertEquals(
      listOf("org.jetbrains.compose.material:material-icons-core"),
      plan.kept.keys.map { it.id },
    )
  }

  @Test
  fun `adds nothing for a removal that can't be made`() {
    val removal = removal("testImplementation", "androidx.test:core-ktx")
    val plan =
      rake(
        advice = listOf(removal, addition("testImplementation", "androidx.test:core")),
        edges =
          mapOf(
            ROOT to setOf("androidx.test:core-ktx"),
            "androidx.test:core-ktx" to setOf("androidx.test:core"),
          ),
        declarations = mapOf("testImplementation" to setOf("androidx.test:core-ktx")),
        unremovable = mapOf(removal to "not found"),
      )

    assertEquals(emptyList(), plan.removals)
    assertEquals(emptyList(), plan.additions)
    assertEquals(mapOf(removal to "not found"), plan.kept)
  }

  @Test
  fun `keeps a test declaration of a dependency main declares compileOnly`() {
    val plan =
      rake(
        advice = listOf(removal("testImplementation", "androidx.compose.foundation:foundation")),
        edges = mapOf(ROOT to setOf("androidx.compose.foundation:foundation")),
        declarations =
          mapOf(
            "compileOnly" to setOf("androidx.compose.foundation:foundation"),
            "testImplementation" to setOf("androidx.compose.foundation:foundation"),
          ),
      )

    assertEquals(emptyList(), plan.removals)
    assertEquals(
      listOf("androidx.compose.foundation:foundation"),
      plan.kept.keys.map { it.id },
    )
  }

  private fun rake(
    advice: List<Advice>,
    edges: Map<String, Set<String>>,
    declarations: Map<String, Set<String>>,
    unremovable: Map<Advice, String> = emptyMap(),
  ): RakePlan =
    DependencyRake(
        advice = advice,
        graphs = listOf(Graph(CLASSPATH, ROOT, edges)),
        classpathConfigurations = mapOf(CLASSPATH to setOf("implementation", "testImplementation")),
        declarations = declarations,
        notations = notations,
      )
      .plan(unremovable)

  private fun removal(configuration: String, id: String) =
    Advice(Coordinate("module", id), from = configuration, to = null)

  private fun addition(configuration: String, id: String) =
    Advice(Coordinate("module", id), from = null, to = configuration)

  private companion object {
    const val ROOT = ":lib"
    const val CLASSPATH = "debugUnitTestCompileClasspath"
  }
}
