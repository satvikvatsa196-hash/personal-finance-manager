package com.example.financemanager.dto.request

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class GoalRequest(
    @field:NotBlank(message = "Goal name cannot be blank")
    val goalName: String?,

    @field:NotNull(message = "Target amount is required")
    @field:Positive(message = "Target amount must be positive")
    val targetAmount: BigDecimal?,

    @field:NotNull(message = "Target date is required")
    @field:Future(message = "Target date must be a future date")
    val targetDate: LocalDate?,

    val startDate: LocalDate?
)

data class GoalUpdateRequest(
    @field:Positive(message = "Target amount must be positive")
    val targetAmount: BigDecimal?,

    @field:Future(message = "Target date must be a future date")
    val targetDate: LocalDate?
)
