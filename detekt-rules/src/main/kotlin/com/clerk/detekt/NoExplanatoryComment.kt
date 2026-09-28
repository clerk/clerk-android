package com.clerk.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.lexer.KtTokens

/**
 * Reports `//` and `/* */` comments. KDoc and `// region` / `// endregion` markers are allowed. See
 * AGENTS.md.
 */
class NoExplanatoryComment(config: Config = Config.empty) : Rule(config) {
  override val issue =
    Issue(
      id = javaClass.simpleName,
      severity = Severity.Style,
      description =
        "Don't write comments that explain code. Put intent in names, types, and tests. See AGENTS.md.",
      debt = Debt.FIVE_MINS,
    )

  override fun visitComment(comment: PsiComment) {
    super.visitComment(comment)
    val isCodeComment =
      comment.tokenType == KtTokens.EOL_COMMENT || comment.tokenType == KtTokens.BLOCK_COMMENT
    if (!isCodeComment || allowedMarker.containsMatchIn(comment.text)) return
    report(CodeSmell(issue, Entity.from(comment), issue.description))
  }

  private companion object {
    val allowedMarker = Regex("""^//\s*(region|endregion)\b""")
  }
}
