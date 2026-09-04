package com.example.financemanager.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class TransactionRequest(
    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal?,

    @field:NotNull(message = "Date is required")
    @field:PastOrPresent(message = "Date cannot be in the future")
    val date: LocalDate?,

    @field:NotBlank(message = "Category is required")
    val category: String?,

    val description: String?
)

data class TransactionUpdateRequest(
    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal?,

    @field:NotBlank(message = "Category is required")
    val category: String?,

    val description: String?
)
