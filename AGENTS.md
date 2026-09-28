# Agent Guidelines

## Code comments

- Do not write comments that explain code. No `//` or `/* */` comments describing what code does or why it does it. Put that intent in names, types, and tests. If behavior depends on a non-obvious constraint, cover it with a test.
- Allowed: KDoc (`/** */`) on declarations, `// region` / `// endregion` markers, and tool directives. Suppress lint findings with `@Suppress` rather than comments.
- When you change behavior that a KDoc comment describes, update the KDoc.
- `./gradlew detekt` enforces this through the `NoExplanatoryComment` rule in `detekt-rules/`.
