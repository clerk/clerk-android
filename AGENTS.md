## Strings

User-facing text is in `source/ui/src/main/res/values/strings.xml`. Each language has its translations in `source/ui/src/main/res/values-<language>/strings.xml`, under the same names.

When you change English text, update the string of the same name in every language to say what the new text says. When you add a string, add it to every language. A string that a language lacks shows in English.

## Comments

Don't write comments unless they explain a constraint from a platform, library,
or spec that we can't change. If our own code needs explaining, change the code:
rename it, give it a type, extract a helper, or pin it with a test.

Keep license headers, doc comments on public API, and `// region` markers.
Don't leave commented-out code.
