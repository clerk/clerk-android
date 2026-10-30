package com.clerk.gradle

/** Line edits to the top-level `dependencies { }` block of a Kotlin build script. */
internal class BuildScriptEdit(lines: List<String>) {
  private val lines = lines.toMutableList()
  private val start: Int
  private var end: Int

  init {
    val open = lines.indexOf("dependencies {")
    val close = if (open < 0) -1 else lines.subList(open, lines.size).indexOf("}")
    start = if (close < 0) -1 else open
    end = if (close < 0) -1 else open + close
  }

  /** Whether the block has a single-line `configuration(notation)`. */
  fun declares(configuration: String, notations: List<String>): Boolean =
    declarationLines(configuration, notations).isNotEmpty()

  /** Removes every single-line `configuration(notation)`; returns whether any was found. */
  fun remove(configuration: String, notations: List<String>): Boolean {
    val matches = declarationLines(configuration, notations)
    matches.asReversed().forEach { lines.removeAt(it) }
    end -= matches.size
    return matches.isNotEmpty()
  }

  private fun declarationLines(configuration: String, notations: List<String>): List<Int> {
    if (start < 0) return emptyList()
    val pattern =
      Regex("""^\s*${Regex.escape(configuration)}\(\s*(${notations.joinToString("|")})\s*\)\s*$""")
    return (start + 1 until end).filter { pattern.matches(lines[it]) }
  }

  /** Adds [declaration] in order among the declarations of the same configuration. */
  fun add(declaration: String) {
    if (start < 0) return
    val line = "  $declaration"
    val configuration = declaration.substringBefore('(')
    val group =
      (start + 1 until end).filter {
        lines[it].startsWith("  $configuration(") &&
          !lines[it].contains("platform(") &&
          lines[it].trimEnd().endsWith(")")
      }
    val index =
      group.firstOrNull { lines[it] > line }
        ?: group.lastOrNull()?.plus(1)
        ?: run {
          if (end - 1 != start && lines[end - 1].isNotBlank()) lines.add(end++, "")
          end
        }
    lines.add(index, line)
    end++
  }

  fun text(): String {
    if (start < 0) return lines.joinToString("\n", postfix = "\n")
    val block =
      lines
        .subList(start + 1, end)
        .filterIndexed { i, line -> line.isNotBlank() || (i > 0 && lines[start + i].isNotBlank()) }
        .dropWhile(String::isBlank)
        .dropLastWhile(String::isBlank)
    return (lines.subList(0, start + 1) + block + lines.subList(end, lines.size)).joinToString(
      "\n",
      postfix = "\n",
    )
  }
}
