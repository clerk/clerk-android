package com.clerk.gradle

/** Line edits to the top-level `dependencies { }` block of a Kotlin build script. */
internal class BuildScriptEdit(lines: List<String>) {
  private val lines = lines.toMutableList()
  private val start = lines.indexOf("dependencies {")
  private var end = if (start < 0) -1 else lines.subList(start, lines.size).indexOf("}") + start

  /** Removes every single-line `configuration(notation)`; returns whether any was found. */
  fun remove(configuration: String, notations: List<String>): Boolean {
    if (start < 0) return false
    val pattern =
      Regex("""^\s*${Regex.escape(configuration)}\(\s*(${notations.joinToString("|")})\s*\)\s*$""")
    val matches = (start + 1 until end).filter { pattern.matches(lines[it]) }
    matches.asReversed().forEach { lines.removeAt(it) }
    end -= matches.size
    return matches.isNotEmpty()
  }

  /** Adds [declaration] in order among the declarations of the same configuration. */
  fun add(declaration: String) {
    if (start < 0) return
    val line = "  $declaration"
    val configuration = declaration.substringBefore('(')
    val group =
      (start + 1 until end).filter {
        lines[it].startsWith("  $configuration(") && !lines[it].contains("platform(")
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
