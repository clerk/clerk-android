package com.clerk.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class ClerkRuleSetProvider : RuleSetProvider {
  override val ruleSetId: String = "clerk"

  override fun instance(config: Config): RuleSet =
    RuleSet(ruleSetId, listOf(NoExplanatoryComment(config)))
}
