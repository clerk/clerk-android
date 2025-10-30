package com.clerk.api.network.model.factor

import com.clerk.api.Constants

internal object FactorComparators {

  val strategySortOrderPasswordPref =
    listOf(
      Constants.Strategy.PASSKEY,
      Constants.Strategy.PASSWORD,
      Constants.Strategy.EMAIL_CODE,
      Constants.Strategy.PHONE_CODE,
    )

  val strategySortOrderOtpPref =
    listOf(
      Constants.Strategy.EMAIL_CODE,
      Constants.Strategy.PHONE_CODE,
      Constants.Strategy.PASSKEY,
      Constants.Strategy.PASSWORD,
    )

  val strategySortOrderAllStrategies =
    listOf(
      Constants.Strategy.EMAIL_CODE,
      Constants.Strategy.PHONE_CODE,
      Constants.Strategy.PASSKEY,
      Constants.Strategy.PASSWORD,
    )

  val strategySortOrderBackupCodePref =
    listOf(Constants.Strategy.TOTP, Constants.Strategy.PHONE_CODE, Constants.Strategy.BACKUP_CODE)

  val passwordPrefComparator: Comparator<Factor> = Comparator { lhs, rhs ->
    val order1 = strategySortOrderPasswordPref.indexOf(lhs.strategy)
    val order2 = strategySortOrderPasswordPref.indexOf(rhs.strategy)
    if (order1 == -1 || order2 == -1) 0 else order1.compareTo(order2)
  }

  val otpPrefComparator: Comparator<Factor> = Comparator { lhs, rhs ->
    val order1 = strategySortOrderOtpPref.indexOf(lhs.strategy)
    val order2 = strategySortOrderOtpPref.indexOf(rhs.strategy)
    if (order1 == -1 || order2 == -1) 0 else order1.compareTo(order2)
  }

  val backupCodePrefComparator: Comparator<Factor> = Comparator { lhs, rhs ->
    val order1 = strategySortOrderBackupCodePref.indexOf(lhs.strategy)
    val order2 = strategySortOrderBackupCodePref.indexOf(rhs.strategy)
    if (order1 == -1 || order2 == -1) 0 else order1.compareTo(order2)
  }

  val allStrategiesButtonsComparator: Comparator<Factor> = Comparator { lhs, rhs ->
    val order1 = strategySortOrderAllStrategies.indexOf(lhs.strategy)
    val order2 = strategySortOrderAllStrategies.indexOf(rhs.strategy)
    if (order1 == -1 || order2 == -1) 0 else order1.compareTo(order2)
  }
}
