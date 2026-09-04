package com.example.financemanager.dto.response

import com.example.financemanager.model.enums.CategoryType
import java.math.BigDecimal
import java.time.LocalDate

data class TransactionDto(
    val id: Long,
    val amount: BigDecimal,
    val date: LocalDate,
    val category: String,
    val description: String?,
    val type: CategoryType
)

data class TransactionListResponse(
    val transactions: List<TransactionDto>
)
