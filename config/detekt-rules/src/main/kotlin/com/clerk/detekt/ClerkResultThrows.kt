package com.clerk.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Flags code that can throw from a public function whose declared return type is `ClerkResult`.
 *
 * `ClerkResult` promises consumers a non-exceptional API: failures must be returned as
 * `ClerkResult.Failure`, not thrown. Inside such a function this rule reports `throw`, `error()`,
 * `require()`, `requireNotNull()`, `check()`, `checkNotNull()`, `TODO()` and `!!`, unless the code
 * sits in a `try` block that catches `Exception`/`Throwable` or in a lambda passed to a catching
 * helper. Rethrowing a caught `CancellationException` is allowed because cancellation must
 * propagate.
 *
 * The rule is syntactic (detekt runs without type resolution here), so it relies on the declared
 * return type and does not follow calls into other functions.
 */
class ClerkResultThrows(config: Config = Config.empty) : Rule(config) {
  override val issue: Issue =
    Issue(
      id = javaClass.simpleName,
      severity = Severity.Defect,
      description =
        "Public functions returning ClerkResult must report failures as ClerkResult.Failure " +
          "instead of throwing.",
      debt = Debt.TWENTY_MINS,
    )

  override fun visitNamedFunction(function: KtNamedFunction) {
    super.visitNamedFunction(function)
    if (function.isLocal || !function.returnsClerkResult() || !function.isPublicApi()) return
    val body = function.bodyExpression ?: return
    body.accept(
      object : KtTreeVisitorVoid() {
        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
          // Members of nested or anonymous classes are checked on their own.
        }

        override fun visitThrowExpression(expression: KtThrowExpression) {
          super.visitThrowExpression(expression)
          if (!expression.rethrowsCancellation() && !expression.isCaught(body)) {
            reportAt(expression, function, "throws")
          }
        }

        override fun visitCallExpression(expression: KtCallExpression) {
          super.visitCallExpression(expression)
          val name = expression.calleeExpression?.text ?: return
          if (name in THROWING_CALLS && !expression.isCaught(body)) {
            reportAt(expression, function, "calls $name()")
          }
        }

        override fun visitPostfixExpression(expression: KtPostfixExpression) {
          super.visitPostfixExpression(expression)
          if (expression.operationToken == KtTokens.EXCLEXCL && !expression.isCaught(body)) {
            reportAt(expression, function, "uses !!")
          }
        }
      }
    )
  }

  private fun reportAt(element: PsiElement, function: KtNamedFunction, what: String) {
    report(
      CodeSmell(
        issue,
        Entity.from(element),
        "${function.name} returns ClerkResult but $what. Return a ClerkResult.Failure instead.",
      )
    )
  }

  private fun KtNamedFunction.returnsClerkResult(): Boolean =
    typeReference?.text?.let { CLERK_RESULT_TYPE.containsMatchIn(it) } == true

  private fun KtNamedFunction.isPublicApi(): Boolean =
    generateSequence<PsiElement>(this) { it.parent }
      .none { element ->
        (element is KtDeclaration && element.hasNonPublicModifier()) ||
          (element is KtNamedFunction && element != this)
      }

  private fun KtDeclaration.hasNonPublicModifier(): Boolean =
    hasModifier(KtTokens.PRIVATE_KEYWORD) ||
      hasModifier(KtTokens.INTERNAL_KEYWORD) ||
      hasModifier(KtTokens.PROTECTED_KEYWORD)

  /** True when a `throw` rethrows the parameter of an enclosing `CancellationException` catch. */
  private fun KtThrowExpression.rethrowsCancellation(): Boolean {
    val thrownName = (thrownExpression as? KtNameReferenceExpression)?.getReferencedName()
    return generateSequence(parent) { it.parent }
      .takeWhile { it !is KtNamedFunction }
      .filterIsInstance<KtCatchClause>()
      .mapNotNull { it.catchParameter }
      .any { parameter ->
        parameter.name == thrownName &&
          parameter.typeReference?.text?.endsWith("CancellationException") == true
      }
  }

  /** True when the element runs inside a broad `try` block or a catching helper's lambda. */
  private fun PsiElement.isCaught(stopAt: PsiElement): Boolean =
    generateSequence(this) { it.parent }
      .takeWhile { it != stopAt }
      .any { element ->
        val parent = element.parent
        (parent is KtTryExpression &&
          element == parent.tryBlock &&
          parent.catchClauses.any { it.catchesBroadly() }) ||
          (element is KtLambdaExpression && element.isPassedToCatchingHelper())
      }

  private fun KtCatchClause.catchesBroadly(): Boolean =
    catchParameter?.typeReference?.text?.substringAfterLast('.') in BROAD_CATCH_TYPES

  private fun KtLambdaExpression.isPassedToCatchingHelper(): Boolean {
    val argument = parent as? KtValueArgument
    // A trailing lambda is a child of the call; a parenthesized one sits in its argument list.
    val call =
      (if (argument is KtLambdaArgument) argument.parent else argument?.parent?.parent)
        as? KtCallExpression
    return call?.calleeExpression?.text in CATCHING_HELPERS
  }

  private companion object {
    val CLERK_RESULT_TYPE = Regex("""^(?:[\w.]+\.)?ClerkResult(?:<|\.|\?|$)""")
    val THROWING_CALLS =
      setOf("error", "require", "requireNotNull", "check", "checkNotNull", "TODO")
    val BROAD_CATCH_TYPES = setOf("Exception", "Throwable")
    val CATCHING_HELPERS = setOf("catchingClerkResult")
  }
}
