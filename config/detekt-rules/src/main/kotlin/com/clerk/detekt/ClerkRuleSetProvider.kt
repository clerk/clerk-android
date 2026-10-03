package com.clerk.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/** Registers the Clerk SDK's repo-specific detekt rules. */
class ClerkRuleSetProvider : RuleSetProvider {
  override val ruleSetId: String = "clerk"

  override fun instance(config: Config): RuleSet =
    RuleSet(ruleSetId, listOf(ClerkResultThrows(config)))
}
