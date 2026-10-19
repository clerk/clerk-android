package com.clerk.detekt

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtValueArgument

class SwallowedCancellation(config: Config = Config.empty) :
  Rule(
    config,
    "Suspend code must not catch CancellationException as a failure: runCatching and " +
      "catch (Exception|Throwable) turn a cancelled coroutine into an error and keep it running.",
  ) {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    if (expression.calleeExpression?.text != "runCatching") return
    val guarded = expression.lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression
    if (guarded?.hasNoCalls() == true) return
    if (!expression.isInSuspendCode() || expression.chainHandlesCancellation()) return
    report(
      Finding(
        Entity.from(expression),
        "runCatching in suspend code also catches CancellationException. Use " +
          "catchingClerkResult { } for ClerkResult code, try/catch with a " +
          "catch (e: CancellationException) { throw e } before the broad catch, or move code " +
          "that never suspends into a non-suspend function.",
      )
    )
  }

  override fun visitCatchSection(catchClause: KtCatchClause) {
    super.visitCatchSection(catchClause)
    val type = catchClause.catchParameter?.typeReference?.text?.substringAfterLast('.')
    val swallows =
      type in BROAD_CATCH_TYPES &&
        !catchClause.cancellationCaughtEarlier() &&
        !catchClause.rethrowsOrChecks()
    val guarded = (catchClause.parent as? KtTryExpression)?.tryBlock
    if (!swallows || guarded?.hasNoCalls() == true || !catchClause.isInSuspendCode()) return
    report(
      Finding(
        Entity.from(catchClause),
        "catch ($type) in suspend code swallows CancellationException. Use " +
          "catchingClerkResult { } for ClerkResult code, add " +
          "catch (e: CancellationException) { throw e } before this clause, or move code " +
          "that never suspends into a non-suspend function.",
      )
    )
  }

  private fun PsiElement.isInSuspendCode(): Boolean {
    val boundary =
      generateSequence(parent) { it.parent }
        .firstOrNull {
          (it is KtCatchClause && it.catchesCancellation()) ||
            (it is KtLambdaExpression && it.isCoroutineBuilderBody()) ||
            it is KtNamedFunction ||
            it is KtClassOrObject
        }
    return (boundary is KtLambdaExpression && !boundary.isNonCancellable()) ||
      (boundary is KtNamedFunction && boundary.hasModifier(KtTokens.SUSPEND_KEYWORD))
  }

  private fun KtLambdaExpression.isCoroutineBuilderBody(): Boolean =
    builderCall()?.calleeExpression?.text in COROUTINE_BUILDERS

  private fun KtLambdaExpression.isNonCancellable(): Boolean {
    val call = builderCall() ?: return false
    return call.calleeExpression?.text == "withContext" &&
      call.valueArguments.firstOrNull()?.getArgumentExpression()?.text == "NonCancellable"
  }

  private fun KtLambdaExpression.builderCall(): KtCallExpression? {
    val argument = parent as? KtValueArgument
    return (if (argument is KtLambdaArgument) argument.parent else argument?.parent?.parent)
      as? KtCallExpression
  }

  private fun KtExpression.hasNoCalls(): Boolean =
    PsiTreeUtil.findChildOfType(this, KtCallExpression::class.java) == null

  private fun KtCallExpression.chainHandlesCancellation(): Boolean {
    var chain: PsiElement = this
    while (chain.parent is KtQualifiedExpression) chain = chain.parent
    return chain.text.contains("CancellationException")
  }

  private fun KtCatchClause.cancellationCaughtEarlier(): Boolean {
    val clauses = (parent as? KtTryExpression)?.catchClauses ?: return false
    return clauses.takeWhile { it != this }.any { it.catchesCancellation() }
  }

  private fun KtCatchClause.catchesCancellation(): Boolean =
    catchParameter?.typeReference?.text?.endsWith("CancellationException") == true

  private fun KtCatchClause.rethrowsOrChecks(): Boolean {
    val body = catchBody as? KtBlockExpression ?: return false
    return body.statements.lastOrNull() is KtThrowExpression ||
      body.text.contains("CancellationException") ||
      body.text.contains("ensureActive")
  }

  private companion object {
    val BROAD_CATCH_TYPES = setOf("Exception", "Throwable")
    val COROUTINE_BUILDERS =
      setOf("launch", "async", "withContext", "coroutineScope", "supervisorScope")
  }
}
