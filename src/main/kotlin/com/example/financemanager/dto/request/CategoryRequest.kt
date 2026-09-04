package com.example.financemanager.dto.request

import com.example.financemanager.model.enums.CategoryType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CategoryRequest(
    @field:NotBlank(message = "Category name cannot be blank")
    val name: String?,

    @field:NotNull(message = "Category type is required")
    val type: CategoryType?
)
