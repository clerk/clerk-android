package com.clerk.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/** Registers the Clerk SDK's repo-specific detekt rules. */
class ClerkRuleSetProvider : RuleSetProvider {
  override val ruleSetId: RuleSetId = RuleSetId("clerk")

  override fun instance(): RuleSet = RuleSet(ruleSetId, listOf(::ClerkResultThrows))
}
