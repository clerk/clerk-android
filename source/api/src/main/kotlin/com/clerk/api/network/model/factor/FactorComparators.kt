package com.clerk.api.network.model.factor

import com.clerk.api.auth.types.Strategy

internal object FactorComparators {

  val strategySortOrderPasswordPref: List<Strategy> =
    listOf(
      Strategy.Passkey,
      Strategy.Password,
      Strategy.EmailLink,
      Strategy.EmailCode,
      Strategy.PhoneCode,
    )

  val strategySortOrderOtpPref: List<Strategy> =
    listOf(
      Strategy.EmailLink,
      Strategy.EmailCode,
      Strategy.PhoneCode,
      Strategy.Passkey,
      Strategy.Password,
    )

  val strategySortOrderAllStrategies: List<Strategy> =
    listOf(
      Strategy.EmailLink,
      Strategy.EmailCode,
      Strategy.PhoneCode,
      Strategy.Passkey,
      Strategy.Password,
    )

  val strategySortOrderBackupCodePref: List<Strategy> =
    listOf(Strategy.Passkey, Strategy.Totp, Strategy.PhoneCode, Strategy.BackupCode)

  val passwordPrefComparator: Comparator<Factor> = Comparator { lhs, rhs ->
    val order1 = strategySortOrderPasswordPref.indexOf(lhs.strategyType)
    val order2 = strategySortOrderPasswordPref.indexOf(rhs.strategyType)
    if (order1 == -1 || order2 == -1) 0 else order1.compareTo(order2)
  }

  val otpPrefComparator: Comparator<Factor> = Comparator { lhs, rhs ->
    val order1 = strategySortOrderOtpPref.indexOf(lhs.strategyType)
    val order2 = strategySortOrderOtpPref.indexOf(rhs.strategyType)
    if (order1 == -1 || order2 == -1) 0 else order1.compareTo(order2)
  }

  val backupCodePrefComparator: Comparator<Factor> = Comparator { lhs, rhs ->
    val order1 = strategySortOrderBackupCodePref.indexOf(lhs.strategyType)
    val order2 = strategySortOrderBackupCodePref.indexOf(rhs.strategyType)
    if (order1 == -1 || order2 == -1) 0 else order1.compareTo(order2)
  }

  val allStrategiesButtonsComparator: Comparator<Factor> = Comparator { lhs, rhs ->
    val order1 = strategySortOrderAllStrategies.indexOf(lhs.strategyType)
    val order2 = strategySortOrderAllStrategies.indexOf(rhs.strategyType)
    if (order1 == -1 || order2 == -1) 0 else order1.compareTo(order2)
  }
}
