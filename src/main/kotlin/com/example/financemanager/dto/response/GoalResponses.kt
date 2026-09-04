package com.example.financemanager.dto.response

import java.math.BigDecimal
import java.time.LocalDate

data class GoalDto(
    val id: Long,
    val goalName: String,
    val targetAmount: BigDecimal,
    val targetDate: LocalDate,
    val startDate: LocalDate,
    val currentProgress: BigDecimal,
    val progressPercentage: BigDecimal,
    val remainingAmount: BigDecimal
)

data class GoalListResponse(
    val goals: List<GoalDto>
)
