package com.example.financemanager.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class TestRequest(
    @field:NotBlank(message = "must not be blank")
    val name: String?,

    @field:Positive(message = "must be positive")
    val amount: BigDecimal?
)
